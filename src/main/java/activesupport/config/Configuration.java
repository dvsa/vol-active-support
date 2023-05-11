package activesupport.config;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.Config;

import java.io.File;

public class Configuration {

    private Config config;

    public Configuration() {
        this.config = ConfigFactory.defaultApplication();
    }

    public Configuration(String environment){

        File tempFile = new File(String.format("%s/src/test/resources/application%s.conf", System.getProperty("user.dir"), environment.toUpperCase()));

        if (tempFile.exists()){
            this.config = ConfigFactory.load(String.format("application%s.conf",environment.toUpperCase()));
        } else {
            this.config = ConfigFactory.defaultApplication();
        }
    }

    public void setConfig(String configName) {
        this.config = ConfigFactory.load(configName);
    }

    public Config getConfig() {
        return config;
    }
}
