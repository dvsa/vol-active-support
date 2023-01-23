package activesupport.s3;

import activesupport.aws.s3.S3;
import activesupport.config.Configuration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.typesafe.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectsTest {

    private static AmazonS3 s3Client;
     static Configuration configuration = new Configuration();

    private static String sesBucketName = "gov-uk-testing-ses-emails";

    static {
        configuration.setConfig("testConfiguration.conf");
        Config conf = configuration.getConfig();
        String accessKey = conf.getString("AWS_ACCESS_KEY_ID");
        String secretKey = conf.getString("AWS_SECRET_KEY");
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

        s3Client = AmazonS3ClientBuilder.standard()
                .withRegion((Regions.EU_WEST_1))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    @Test
    public void testListObjectsByLastModified() {
        String bucketName = sesBucketName;
        String path = "gov_uk_testing_dev-dvsacloud_uk";
        String expected = "Fri Jan 13 13:04:33 GMT 2023";

        String actual = S3.listObjectsByLastModified(bucketName, path);
        System.out.println(actual);
       // assertEquals(expected, actual);
    }

}

