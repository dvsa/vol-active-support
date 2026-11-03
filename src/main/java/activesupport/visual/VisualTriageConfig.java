package activesupport.visual;

import activesupport.aws.s3.SecretsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Settings resolve from system properties ({@code visualTriage.*}) first, then AWS Secrets Manager.
 */
public final class VisualTriageConfig {

    private static final Logger LOGGER = LogManager.getLogger(VisualTriageConfig.class);
    private static final String PREFIX = "visualTriage.";
    private static final String DEFAULT_SECRET_NAME = "vol-functional-tests/bedrock";
    private static final String DEFAULT_EXPECTED_CHANGES =
            "The feedback link moved from the phase banner to a 'Help us improve this service' panel above the footer; "
            + "an English/Cymraeg language switcher was added (GOV.UK Frontend v6 patterns).";

    private VisualTriageConfig() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(PREFIX + "enabled", "false"));
    }

    public static String getRegion() {
        return System.getProperty(PREFIX + "region", "eu-west-1");
    }

    public static String getAgentId() {
        return getConfigValue("agentId", "visualTriage_agentId");
    }

    public static String getAgentAliasId() {
        return getConfigValue("agentAliasId", "visualTriage_agentAliasId");
    }

    /** Also attach the cropped PNGs to the agent session (useCase CHAT); off by default because agents are text-first. */
    public static boolean isAttachImages() {
        return Boolean.parseBoolean(System.getProperty(PREFIX + "attachImages", "false"));
    }

    private static String getConfigValue(String property, String secretKey) {
        String prop = System.getProperty(PREFIX + property);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        try {
            return SecretsManager.getSecretValue(System.getProperty(PREFIX + "secretName", DEFAULT_SECRET_NAME), secretKey);
        } catch (Exception e) {
            LOGGER.warn("VISUAL-TRIAGE: Failed to fetch '{}' from Secrets Manager: {}", secretKey, e.getMessage());
            return "";
        }
    }

    /** Minimum pixel/box movement treated as a layout change in geometry comparison. */
    public static int getGeometryTolerance() {
        return Integer.parseInt(System.getProperty(PREFIX + "geometryTolerance", "3"));
    }

    /** Caps the geometry differences sent per page to keep prompts small. */
    public static int getMaxGeometryDiffs() {
        return Integer.parseInt(System.getProperty(PREFIX + "maxGeometryDiffs", "40"));
    }

    /** Fraction of changed pixels below which a page is treated as unchanged and never sent to Bedrock. */
    public static double getDiffThreshold() {
        return Double.parseDouble(System.getProperty(PREFIX + "diffThreshold", "0.001"));
    }

    /** Per-channel colour delta (0-255) ignored as anti-aliasing noise. */
    public static int getPixelTolerance() {
        return Integer.parseInt(System.getProperty(PREFIX + "pixelTolerance", "24"));
    }

    /** Hard cap on Bedrock calls per run. */
    public static int getMaxCalls() {
        return Integer.parseInt(System.getProperty(PREFIX + "maxCalls", "40"));
    }

    /** Longest edge of each image sent to the model; larger crops are scaled down. */
    public static int getMaxImageEdge() {
        return Integer.parseInt(System.getProperty(PREFIX + "maxImageEdge", "1568"));
    }

    public static String getExpectedChanges() {
        return System.getProperty(PREFIX + "expectedChanges", DEFAULT_EXPECTED_CHANGES);
    }
}
