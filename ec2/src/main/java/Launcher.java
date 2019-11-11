

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

/**
 * 
 * @author Alexander Lercher
 * @author Thomas Auer
 * 
 * Extended 2019, D.K. for DCI
 *
 */
public class Launcher {
static String bucketName="bucket"+Math.random();
static AmazonS3 s3;
static String key="key";
    public static void main(String[] args) throws IOException, InterruptedException {

    	/***************** Load the credentials ****************/
		AWSCredentialsProvider credentials = null;
		try {
			credentials = new ProfileCredentialsProvider("default");
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (/home/dragi/.aws/credentials), and is in valid format.", e);
		}
        
        /*****************Set the availability zones****************/

        s3 = AmazonS3ClientBuilder
                .standard()
                .withCredentials(credentials)
                .withRegion(Regions.EU_CENTRAL_1)
                .build();

        /*****************Create bucket - NOTE: the bucked should be unique for all Amazon users****************/
        try {
            System.out.println("Creating bucket " + bucketName + "\n");
            s3.createBucket(bucketName);

            /*
             * List all available buckets in your account
             */
            System.out.println("List the buckets");
            for (Bucket bucket : s3.listBuckets()) {
                System.out.println(" ->> " + bucket.getName());
            }
            System.out.println();

            /*****************Upload new object in the bucket****************/

            File textFile=createSampleFile();
            ObjectMetadata objectMetadata=new ObjectMetadata();
            objectMetadata.setContentType("plain/text");
            PutObjectRequest putObjectRequest=new PutObjectRequest(bucketName,key,textFile).withMetadata(objectMetadata);
            s3.putObject(putObjectRequest);
            /*****************List all objects in the bucket****************/
            System.out.println("Listing objects");
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                    .withBucketName(bucketName));
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                System.out.println(" - " + objectSummary.getKey() + "  " +
                        "(size = " + objectSummary.getSize() + ")");
            }
            System.out.println();

            
            /*****************Download file from bucket****************/
            System.out.println("Downloading file");
            File localF = new File("file.txt");
    		s3.getObject(new GetObjectRequest(bucketName, key), localF);
            Thread.sleep(10000);
            System.out.println();


            /*****************Delete file from bucket****************/

            s3.deleteObject(bucketName,key);

            /*****************Delete all files from bucket****************/
            objectListing = s3.listObjects(new ListObjectsRequest()
                    .withBucketName(bucketName));
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                s3.deleteObject(bucketName,objectSummary.getKey());
            }


            /*****************Delete bucket****************/
            System.out.println("Deleting bucket");
    		s3.deleteBucket(new DeleteBucketRequest(bucketName));
            Thread.sleep(10000);
                        
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    /**
     * Creates a temporary file with text data to demonstrate uploading a file
     * to Amazon S3. This code was utilized from the Amazon S3 wizard. This method is was created by Amazon Inc.
     *
     * @return A newly created temporary file with text data.
     *
     * @throws IOException
     */

    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-proseminar", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("This is a test file for the\n");
        writer.write("Proseminar in APDS\n");
        writer.close();

        return file;
    }
}
