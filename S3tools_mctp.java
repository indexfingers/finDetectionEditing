package benWhitesharkPkg;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;


public class S3tools_mctp {
	
	
	private AmazonS3 s3Client;
	private AWSCredentials credentials;
	private boolean isCredentials;
	
	
	
	public void initialise() {
		//assumes relying on iam role attached to ec2 instance to provide credentials 
		s3Client = new AmazonS3Client();
	}
	
	
	public void initialise(String userAtAccount) {
		try {
			try {
				this.credentials = new ProfileCredentialsProvider(userAtAccount).getCredentials();
				this.isCredentials = true;
			} catch (Exception e) {
				isCredentials = false;
				throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
						+ "Please make sure that your credentials file is at the correct "
						+ "location (C:\\Users\\Ben\\.aws\\credentials), and is in valid format.", e);
			}
		} catch (Exception e2) {
			isCredentials = false;
		}
		
		
		if (this.isCredentials == true) {
			s3Client = new AmazonS3Client(credentials);

		} else {
			s3Client = new AmazonS3Client();
		}
	}
	
	
	public String downloadJsonString(String bucket, String key) {
		return s3Client.getObjectAsString(bucket, key);
	}
	
	
	public void uploadJsonString(String bucket,String key,String jsonStr) {
		s3Client.putObject(bucket, key, jsonStr);
	}
	
	

}
