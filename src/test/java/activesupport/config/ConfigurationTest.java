package activesupport.config;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigurationTest {

    @Test
    public void getConfig() {
        Configuration config = new Configuration();
        String configName = "testConfiguration";
        config.setConfig(configName);
        assertEquals("testUser", config.getConfig().getString("dbUsername"));
    }
}