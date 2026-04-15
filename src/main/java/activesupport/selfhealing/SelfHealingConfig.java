package activesupport.selfhealing;

import activesupport.aws.s3.SecretsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SelfHealingConfig {

    private static final Logger LOGGER = LogManager.getLogger(SelfHealingConfig.class);
    private static final String PREFIX = "selfHealing.";
    private static final String DEFAULT_SECRET_NAME = "vol-functional-tests/bedrock";

    private SelfHealingConfig() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(PREFIX + "enabled", "false"));
    }

    public static String getRegion() {
        return System.getProperty(PREFIX + "region", "eu-west-1");
    }

    public static String getAgentId() {
        return getConfigValue("selfHeal_agentId");
    }

    public static String getAgentAliasId() {
        return getConfigValue("selfHealth_agentAliasId");
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
        try {
            String secretName = System.getProperty(PREFIX + "secretName", DEFAULT_SECRET_NAME);
            return SecretsManager.getSecretValue(secretName, key);
        } catch (Exception e) {
            LOGGER.warn("SELF-HEALING: Failed to fetch '{}' from Secrets Manager: {}", key, e.getMessage());
            return "";
        }
    }
}
