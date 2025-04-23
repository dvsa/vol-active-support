package activesupport.aws.s3;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.secretsmanager.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecretsManager {

    private static final Logger LOGGER = LogManager.getLogger(SecretsManager.class);

    private static volatile AWSSecretsManager secretsManager;
    private static volatile String secretsId;

    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    private static AWSSecretsManager getSecretsManager() {
        if (secretsManager == null) {
            synchronized (SecretsManager.class) {
                if (secretsManager == null) {
                    LOGGER.info("Initializing AWSSecretsManager client...");
                    secretsManager = AWSSecretsManagerClientBuilder
                            .standard()
                            .withCredentials(new DefaultAWSCredentialsProviderChain())
                            .withRegion(Regions.EU_WEST_1.getName()) // Explicitly set the region
                            .build();
                }
            }
        }
        return secretsManager;
    }

    private static String getSecretsId() {
        if (secretsId == null) {
            synchronized (SecretsManager.class) {
                if (secretsId == null) {
                    LOGGER.info("Fetching secretsId...");
                    secretsId = fetchSecretName();
                }
            }
        }
        return secretsId;
    }

    private static String fetchSecretName() {
        AWSSecretsManager secretsManager = getSecretsManager();
        ListSecretsRequest listSecretsRequest = new ListSecretsRequest();
        ListSecretsResult listSecretsResult = secretsManager.listSecrets(listSecretsRequest);

        try {
            for (SecretListEntry secret : listSecretsResult.getSecretList()) {
                if (secret.getName().trim().equalsIgnoreCase("RUNNER-MAIN-APPLICATION")) {
                    LOGGER.info("Secret found: " + secret.getName());
                    LOGGER.info("Secret ARN: " + secret.getARN());
                    LOGGER.info("Secret Description: " + secret.getDescription());
                    return secret.getName();
                }
            }
        } catch (AmazonServiceException e) {
            LOGGER.error("AWS service error occurred: " + e.getMessage());
            throw new RuntimeException("Failed to connect to AWS Secrets Manager", e);
        } catch (SdkClientException e) {
            LOGGER.error("AWS SDK client error occurred: " + e.getMessage());
            throw new RuntimeException("Failed to authenticate with AWS Secrets Manager", e);
        }

        throw new RuntimeException("No secret found ending with RUNNER-MAIN-APPLICATION");
    }

    public static String getSecretValue(String secretKey) {
        if (cache.containsKey(secretKey)) {
            return cache.get(secretKey);
        }

        String secret = null;

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                .withSecretId(getSecretsId());
        GetSecretValueResult getSecretValueResult = null;

        try {
            getSecretValueResult = getSecretsManager().getSecretValue(getSecretValueRequest);
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
            getSecretsManager().updateSecret(updateSecretRequest);
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
            getSecretsManager().createSecret(request);
            LOGGER.info("Secret has been set");
        } catch (ResourceExistsException e) {
            LOGGER.info("The secret key '" + secretId + "'  already exists... " +
                    "please use the updateSecretKey method instead or use a new key");
        }
    }
}
