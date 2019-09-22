package com.apibuilder.storage;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;

public class AwsSystemsManagerClient {

	AWSSimpleSystemsManagement ssm;
	public AwsSystemsManagerClient() {
		// TODO Auto-generated constructor stub
		/**
		* Initialize AWS System Manager Client with default credentials
		*/
		ssm = AWSSimpleSystemsManagementClient.builder()
				.withCredentials(new ProfileCredentialsProvider()).withRegion(Regions.US_WEST_2).build();
	}
	
	/**
     * Get parameter from SSM, with or without encryption (use IAM role for decryption)
     * Throws {@Link com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException} if not found
     * @param key
     * @param encryption
     * @return value
     */
    public String getParameter(String key, boolean encryption) {
        GetParameterRequest getparameterRequest = new GetParameterRequest().withName(key).withWithDecryption(encryption);
        final GetParameterResult result = ssm.getParameter(getparameterRequest);
        return result.getParameter().getValue();
    }
}
