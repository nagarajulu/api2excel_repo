package com.apibuilder.storage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;

@Service
public class S3BucketStorageService {
	 private final Path rootLocation;
	 private final String USER_HOME=System.getProperty("user.home");
	 
    @Autowired
    public S3BucketStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }
	    
    public Path getRootLocation() {
    	return rootLocation;
    }
    
    public String getUserHomeDirectory() {
    	return USER_HOME;
    }
	private static String bucketName     = "api2excel";
	
	/**
	 * Stores file in S3 buckets and returns URL associated to it.
	 * @param file
	 * @return
	 */
	public String storeToS3Bucket(File file) {
		String url=null;
		
		
        try {
        	
    		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
    		                        .withCredentials(new ProfileCredentialsProvider())
    		                        .withRegion(Regions.US_WEST_2)
    		                        .build();
    		
            /*AmazonS3 s3Client =AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.US_WEST_2)
                    .build();;*/
    		if(!(s3Client.doesBucketExistV2(bucketName)))
            {
            	// Note that CreateBucketRequest does not specify region. So bucket is 
            	// created in the region specified in the client.
    			s3Client.createBucket(new CreateBucketRequest(
						bucketName));
            }
    		
    		//set public access for bucket
    		s3Client.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
    		//set public read access for all files within bucket, so users can download API documents.
    		grantPublicReadAccess(s3Client);
    		
            // Get location.
            String bucketLocation = s3Client.getBucketLocation(new GetBucketLocationRequest(bucketName));
            System.out.println("bucket location = " + bucketLocation);
            
            System.out.println("Uploading a new object to S3 from a file\n");
            //File file = new File(uploadFileName);
            /*s3Client.putObject(new PutObjectRequest(
            		                 bucketName, keyName, file));*/
            
         // Create a list of UploadPartResponse objects. You get one of these
            // for each part upload.
            List<PartETag> partETags = new ArrayList<PartETag>();

            // Step 1: Initialize.
            InitiateMultipartUploadRequest initRequest = new 
                 InitiateMultipartUploadRequest(bucketName, file.getName());
            InitiateMultipartUploadResult initResponse = 
            	                   s3Client.initiateMultipartUpload(initRequest);

            long contentLength = file.length();
            long partSize = 5242880; // Set part size to 5 MB.

            try {
                // Step 2: Upload parts.
                long filePosition = 0;
                for (int i = 1; filePosition < contentLength; i++) {
                    // Last part can be less than 5 MB. Adjust part size.
                	partSize = Math.min(partSize, (contentLength - filePosition));
                	
                    // Create request to upload a part.
                    UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucketName).withKey(file.getName())
                        .withUploadId(initResponse.getUploadId()).withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(partSize);

                    // Upload part and add response to our list.
                    partETags.add(
                    		s3Client.uploadPart(uploadRequest).getPartETag());

                    filePosition += partSize;
                }

                // Step 3: Complete.
                CompleteMultipartUploadRequest compRequest = new 
                             CompleteMultipartUploadRequest(
                                        bucketName, 
                                        file.getName(), 
                                        initResponse.getUploadId(), 
                                        partETags);

                
                s3Client.completeMultipartUpload(compRequest);
            } catch (Exception e) {
                s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
                        bucketName, file.getName(), initResponse.getUploadId()));
                System.out.println("S3BucketStorageService.storeToS3Bucket()--ERROR ABORTING");
                throw e;
            }
            
            
            
            url=s3Client.getUrl(bucketName, file.getName()).toString();
            
            System.out.println("S3BucketStorageService.storeToS3Bucket():"+url);

         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
        return url;
	}//end of method

	
	void grantPublicReadAccess(AmazonS3 s3Client) {
		Statement allowPublicReadStatement = new Statement(Effect.Allow)
			    .withPrincipals(Principal.AllUsers)
			    .withActions(S3Actions.GetObject)
			    .withResources(new S3ObjectResource(bucketName, "*"));
		
		Policy policy = new Policy()
			    .withStatements(allowPublicReadStatement);
		
		s3Client.setBucketPolicy(bucketName, policy.toJson());
	}
	
}
