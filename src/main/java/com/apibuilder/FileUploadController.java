package com.apibuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.wsdl.WSDLException;

import org.apache.tomcat.util.http.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.apibuilder.json.JSONBuilder;
import com.apibuilder.storage.FileObj;
import com.apibuilder.storage.S3BucketStorageService;
import com.apibuilder.storage.StorageFileNotFoundException;
import com.apibuilder.storage.StorageService;
import com.apibuilder.util.ParseResult;
import com.apibuilder.wsdl.WSDLBuilder;

@Controller
public class FileUploadController implements ErrorController {
	
	private static final String PATH = "/error";
	private static final String HOME_PATH="redirect:/#services";

    @RequestMapping(value = PATH)
    public String error(Model model, RedirectAttributes redirectAttributes) {
    	
		return "index.html#services";
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }

    private final StorageService storageService;
    private final S3BucketStorageService s3StorageService;
    
    @Autowired
    public FileUploadController(StorageService storageService, S3BucketStorageService s3StorageService) {
        this.storageService = storageService;
        this.s3StorageService = s3StorageService;
    }

    @GetMapping("/")
    public String returnHome(Model model) throws IOException {
    	
        return "index.html";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile[] files,
            RedirectAttributes redirectAttributes) {

    	final String requestUuid = UUID.randomUUID().toString();
		Path tempDirPath=Paths.get(s3StorageService.getUserHomeDirectory(), requestUuid);
    	try {
			Files.createDirectory(tempDirPath);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			redirectAttributes.addFlashAttribute("message", "ERROR OCCURRED. ");
			return HOME_PATH;
		}
    	//store uploaded files in temporary directory
    	for(MultipartFile file : files) {
    		File targetFile= new File(tempDirPath.toString(), file.getOriginalFilename());
    		try {
				file.transferTo(targetFile);
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				redirectAttributes.addFlashAttribute("message", "ERROR OCCURRED IN PARSING "+file.getOriginalFilename());
				return HOME_PATH;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				redirectAttributes.addFlashAttribute("message", "ERROR OCCURRED IN PARSING "+file.getOriginalFilename());
				return HOME_PATH;
			}
    	}
    	
    	for(MultipartFile file : files) {
    		final Path fileUri = Paths.get(tempDirPath.toString(), file.getOriginalFilename());
			File localFile=fileUri.toFile();
	    	if(file.getOriginalFilename().endsWith(".wsdl") || file.getContentType()=="text/xml") {
	    		//API Builder source code
	        	WSDLBuilder apiBuilder = new WSDLBuilder();
	        	ParseResult pr;
	        	try {
					pr=apiBuilder.parseWSDL(fileUri.toString(), localFile, s3StorageService, tempDirPath);
					//clean up temp directory
					File[] dirFiles=tempDirPath.toFile().listFiles();
					for (File dirFile : dirFiles) {
			           dirFile.delete();
			        }
					tempDirPath.toFile().delete();
				} catch (WSDLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					redirectAttributes.addFlashAttribute("message", "ERROR OCCURRED IN PARSING "+file.getOriginalFilename());
					return HOME_PATH;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					redirectAttributes.addFlashAttribute("message", "ERROR OCCURRED IN PARSING "+file.getOriginalFilename());
					return HOME_PATH;
				}
	        	catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					redirectAttributes.addFlashAttribute("message", "ERROR OCCURRED IN PARSING "+file.getOriginalFilename());
					return HOME_PATH;
				}
	        	//
	            //storageService.store(file);
	        	
	        	//redirectAttributes.addFlashAttribute("message", pr.getUiMsg().getMessage());
	    		redirectAttributes.addFlashAttribute("successMsg", "WSDL file successfully uploaded "+file.getOriginalFilename() + " and APIs are generated for the operations. Please download below");
	    		redirectAttributes.addFlashAttribute("apiUriList", pr.getFileURIs());
	    	}
	    	else if(file.getOriginalFilename().endsWith(".json") || file.getContentType()=="application/json") {
	    		//API Builder source code
	        	JSONBuilder apiBuilder  = new JSONBuilder();
	        	try {
	        		ParseResult pr=apiBuilder.parseJSONFile(localFile, s3StorageService, tempDirPath);    
	        		
	        		redirectAttributes.addFlashAttribute("successMsg", pr.getUiMsg().getMessage());
		        	redirectAttributes.addFlashAttribute("apiUriList", pr.getFileURIs());
		        	//
		            //storageService.store(file);
		        	//clean up temp directory
					File[] dirFiles=tempDirPath.toFile().listFiles();
					for (File dirFile : dirFiles) {
			           dirFile.delete();
			        }
					tempDirPath.toFile().delete();
	        	}
	        	catch(Exception e){
	        		e.printStackTrace();
	        		redirectAttributes.addFlashAttribute("message", "ERROR OCCURRED IN PARSING "+file.getOriginalFilename());
					return HOME_PATH;
	        	}
	    	}
	    	else if(file.getOriginalFilename().endsWith(".xsd")) {
	    		continue;
	    	}
	    	else {
	    		redirectAttributes.addFlashAttribute("message", "File uploaded by you "+file.getOriginalFilename()+" is not matching the supported formats: WSDL, Swagger(.JSON) ");
	    		return HOME_PATH;
	    	}
    	}

        return HOME_PATH;
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    //@RequestMapping(value = PATH)
    @ExceptionHandler(FileSizeLimitExceededException.class)
    public String uploadedAFileTooLarge(FileSizeLimitExceededException e) {
        return "FILE SIZE EXCEEDED";
    }
}
