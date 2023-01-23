package activesupport.aws.s3;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecretsManager {

    public String amazonResourceName;
    public String region;

    public String getAmazonResourceName(){
        return amazonResourceName;
    }

    public String getRegion(){
        return region;
    }

    public void setAmazonResourceName(String amazonResourceName){
        this.amazonResourceName = amazonResourceName;
    }

    public void setRegion(String region){
        this.region = region;
    }

    private static final Logger LOGGER = LogManager.getLogger(SecretsManager.class);

    public AWSSecretsManager awsClientSetup(){
        return AWSSecretsManagerClientBuilder
                .standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(region)
                .build();
    }

    public String getSecretValue(String secretKey) {
        String secret = null;

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                .withSecretId(secretKey);
        GetSecretValueResult getSecretValueResult = null;

        try {
            getSecretValueResult = awsClientSetup().getSecretValue(getSecretValueRequest);

        } catch (ResourceNotFoundException e) {
            LOGGER.info("The requested secret " + secretKey + " was not found");
        } catch (InvalidRequestException e) {
            LOGGER.info("The request was invalid due to: " + e.getMessage());
        } catch (InvalidParameterException e) {
            LOGGER.info("The request had invalid params: " + e.getMessage());
        }

        assert getSecretValueResult != null;

        if (getSecretValueResult.getSecretString() != null) {
            secret = getSecretValueResult.getSecretString();
        }
        return secret;
    }

    public void updateSecret(String secretId, String secretValue) {
        try {
            UpdateSecretRequest updateSecretRequest = new UpdateSecretRequest()
                    .withSecretId(secretId)
                    .withSecretString(String.format("{password:%s}", secretValue));
            awsClientSetup().updateSecret(updateSecretRequest);
        } catch (AWSSecretsManagerException e) {
            LOGGER.info(" You've either entered an Invalid name. 1) Must be a valid name containing alphanumeric characters, or any of the following: -/_+=.@!" +
                    "or 2)The secretId '" + secretId + "' does not exist");
        }
    }

    public void setSecretKey(String secretId, String secretValue) {
        try {
            CreateSecretRequest request = new CreateSecretRequest()
                    .withDescription("password for testing")
                    .withName(secretId)
                    .withSecretString(String.format("{password:%s}", secretValue));
            awsClientSetup().createSecret(request);
            LOGGER.info("Secret has been set");
        } catch (ResourceExistsException e) {
            LOGGER.info("The secret key '" + secretId + "'  already exists... " +
                    "please use the updateSecretKey method instead or use a new key");
        }
    }
}