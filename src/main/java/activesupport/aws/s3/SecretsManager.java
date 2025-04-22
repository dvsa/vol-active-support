package activesupport.aws.s3;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecretsManager {

    private static final Logger LOGGER = LogManager.getLogger(SecretsManager.class);

    public static String secretsId = getSecretName();

    private static String getSecretName() {
        String accountId = getAccountId();
        return String.format("%sRUNNER-MAIN-APPLICATION", accountId);
    }

    private static String getAccountId() {
        try {
            GetCallerIdentityRequest request = new GetCallerIdentityRequest();
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder
                    .defaultClient();
            GetCallerIdentityResult response = stsClient.getCallerIdentity(request);
            return response.getAccount();
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve AWS account ID: " + e.getMessage());
            throw new RuntimeException("Unable to determine AWS account ID", e);
        }
    }

    private static final Map<String, String> cache = new ConcurrentHashMap<>();
    private static final AWSSecretsManager secretsManager = awsClientSetup();

    private static AWSSecretsManager awsClientSetup() {
        Regions region = Regions.EU_WEST_1;
        return AWSSecretsManagerClientBuilder
                .standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(region)
                .build();
    }

    public static String getSecretValue(String secretKey) {
        if (cache.containsKey(secretKey)) {
            return cache.get(secretKey);
        }

        String secret = null;

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                .withSecretId(secretsId);
        GetSecretValueResult getSecretValueResult = null;

        try {
            getSecretValueResult = secretsManager.getSecretValue(getSecretValueRequest);
        } catch (ResourceNotFoundException e) {
            LOGGER.info("The requested secret " + secretKey + " was not found");
        } catch (InvalidRequestException e) {
            LOGGER.info("The request was invalid due to: " + e.getMessage());
        } catch (InvalidParameterException e) {
            LOGGER.info("The request had invalid params: " + e.getMessage());
        }

        if (getSecretValueResult != null && getSecretValueResult.getSecretString() != null) {
            secret = getSecretValueResult.getSecretString();
            JSONObject jsonObject = new JSONObject(secret);
            secret = jsonObject.getString(secretKey);
            cache.put(secretKey, secret);
        }
        return secret;
    }

    public static void updateSecret(String secretId, String secretValue) {
        try {
            UpdateSecretRequest updateSecretRequest = new UpdateSecretRequest()
                    .withSecretId(secretId)
                    .withSecretString(String.format("{password:%s}", secretValue));
            secretsManager.updateSecret(updateSecretRequest);
        } catch (AWSSecretsManagerException e) {
            LOGGER.info(
                    " You've either entered an Invalid name. 1) Must be a valid name containing alphanumeric characters, or any of the following: -/_+=.@!"
                            +
                            "or 2)The secretId '" + secretId + "' does not exist");
        }
    }

    public static void setSecretKey(String secretId, String secretValue) {
        try {
            CreateSecretRequest request = new CreateSecretRequest()
                    .withDescription("password for testing")
                    .withName(secretId)
                    .withSecretString(String.format("{password:%s}", secretValue));
            secretsManager.createSecret(request);
            LOGGER.info("Secret has been set");
        } catch (ResourceExistsException e) {
            LOGGER.info("The secret key '" + secretId + "'  already exists... " +
                    "please use the updateSecretKey method instead or use a new key");
        }
    }
}