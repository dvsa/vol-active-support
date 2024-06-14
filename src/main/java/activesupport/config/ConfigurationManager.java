package activesupport.config;

import activesupport.aws.s3.SecretsManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ConfigurationManager {
    private static final Config config;

    static {
        config = ConfigFactory.load();
    }

    public static Config getConfig() {
        return config;
    }

    public static String getSecret(String key) {
        return SecretsManager.getSecretValue(key);
    }
}
