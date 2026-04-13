package activesupport.selfhealing;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SelfHealingConfig {

    private static final Logger LOGGER = LogManager.getLogger(SelfHealingConfig.class);
    private static final String PREFIX = "selfHealing.";
    private static final String DEFAULT_SECRET_NAME = "vol-functional-tests/self-healing";
    private static final Map<String, String> secretCache = new ConcurrentHashMap<>();

    private SelfHealingConfig() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(PREFIX + "enabled", "false"));
    }

    public static String getRegion() {
        return System.getProperty(PREFIX + "region", "eu-west-1");
    }

    public static String getAgentId() {
        return getConfigValue("agentId");
    }

    public static String getAgentAliasId() {
        return getConfigValue("agentAliasId");
    }

    public static int getMaxDomLength() {
        return Integer.parseInt(System.getProperty(PREFIX + "maxDomLength", "50000"));
    }

    public static double getMinConfidence() {
        return Double.parseDouble(System.getProperty(PREFIX + "minConfidence", "0.5"));
    }

    /**
     * Resolves a config value: system property first, then AWS Secrets Manager.
     */
    private static String getConfigValue(String key) {
        String prop = System.getProperty(PREFIX + key);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return getSecretValue(key);
    }

    private static String getSecretValue(String key) {
        if (secretCache.containsKey(key)) {
            return secretCache.get(key);
        }

        try {
            String secretName = System.getProperty(PREFIX + "secretName", DEFAULT_SECRET_NAME);
            AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
                    .withCredentials(new DefaultAWSCredentialsProviderChain())
                    .withRegion(Regions.EU_WEST_1)
                    .build();

            GetSecretValueResult result = client.getSecretValue(
                    new GetSecretValueRequest().withSecretId(secretName));

            if (result.getSecretString() != null) {
                JsonObject json = JsonParser.parseString(result.getSecretString()).getAsJsonObject();
                if (json.has(key)) {
                    String value = json.get(key).getAsString();
                    secretCache.put(key, value);
                    return value;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("SELF-HEALING: Failed to fetch '{}' from Secrets Manager: {}", key, e.getMessage());
        }

        return "";
    }
}
