package com.apibuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

import com.apibuilder.storage.FileObj;
import com.apibuilder.storage.StorageFileNotFoundException;
import com.apibuilder.storage.StorageService;
import com.swagger.apibuilder.APIBuilder;
import com.swagger.apibuilder.ParseResult;
import com.wsdl.isdbuilder.ISDBuilder;

@Controller
public class FileUploadController implements ErrorController {
	
	private static final String PATH = "/error";

    @RequestMapping(value = PATH)
    public String error(Model model) {
    	model.addAttribute("message", " FILE SIZE EXCEEDED or OTHER ERROR OCCURRED.");
    	
		return "uploadForm";
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {
    	
    	List<String> paths=storageService.loadAll().map(
                path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "serveFile", path.getFileName().toString()).build().toString())
                .collect(Collectors.toList());
    	
    	List<FileObj> filesList=new ArrayList<FileObj>();
    	
    	for(String path: paths) {
    		FileObj f=new FileObj();
    		f.setFilename(path.substring(path.lastIndexOf("/")+1));
    		f.setPath(path);
    		filesList.add(f);
    	}

        model.addAttribute("files", filesList);

        return "uploadForm";
    }

   // @GetMapping("/error")
    //@ExceptionHandler(FileSizeLimitExceededException.class)
    public String handleError(Model model) throws IOException {
    	
    	//model.addAttribute("message", " FILE SIZE EXCEEDED or OTHER ERROR OCCURRED.");
    	
		return "uploadForm";    	
    }
    
    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

    	if(file.getOriginalFilename().endsWith(".wsdl") || file.getContentType()=="text/xml") {
    		//API Builder source code
        	ISDBuilder apiBuilder = new ISDBuilder();
        	try {
				apiBuilder.parseWSDL("", file, storageService);
			} catch (WSDLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				redirectAttributes.addFlashAttribute("message", " ERROR OCCURRED.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				redirectAttributes.addFlashAttribute("message", " ERROR OCCURRED.");
			}    	
        	//
            //storageService.store(file);
        	
        	//redirectAttributes.addFlashAttribute("message", pr.getUiMsg().getMessage());
    		redirectAttributes.addFlashAttribute("message", " WSDL file uploaded."+file.getOriginalFilename());
    		
    	}
    	else if(file.getOriginalFilename().endsWith(".json") || file.getContentType()=="application/json") {
    		//API Builder source code
        	APIBuilder apiBuilder = new APIBuilder();
        	ParseResult pr=apiBuilder.parseJSONFile("", file, storageService);    	
        	//
            //storageService.store(file);
        	
        	redirectAttributes.addFlashAttribute("message", pr.getUiMsg().getMessage());
        	redirectAttributes.addFlashAttribute("apiUriList", pr.getURI());
    	}
    	else {
    		redirectAttributes.addFlashAttribute("message", " Neither WSDL nor JSON file uploaded. Please try with one of these file formats "+file.getOriginalFilename());
    	}
    	

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(FileSizeLimitExceededException.class)
    public String uploadedAFileTooLarge(FileSizeLimitExceededException e) {
        return "FILE SIZE EXCEEDED";
    }
}
