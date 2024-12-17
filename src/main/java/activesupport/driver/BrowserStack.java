package activesupport.driver;

import activesupport.config.Configuration;
import com.browserstack.local.Local;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BrowserStack {

    //DEPRECATED
    private static final Logger LOGGER = LogManager.getLogger(BrowserStack.class);
    static Local browserStack = new Local();

    public static Map<String, String> localArgs() {
        Configuration config = new Configuration();
        Map<String, String> bsLocalArgs = new HashMap<>();
        bsLocalArgs.put("v", "true");
        bsLocalArgs.put("logFile", config.getConfig().getString("browserStackLogLocation"));
        bsLocalArgs.put("skipCheck", "true");
        bsLocalArgs.put("exclude-hosts", ".*.signin.service.gov.uk");
        bsLocalArgs.put("force", "true");
        bsLocalArgs.put("forcelocal", "true");
        bsLocalArgs.put("localIdentifier", "vol");
        bsLocalArgs.put("key", config.getConfig().getString("browserStackKey"));
        bsLocalArgs.put("proxyHost", config.getConfig().getString("proxyHost"));
        bsLocalArgs.put("proxyPort", config.getConfig().getString("proxyPort"));
        if (!config.getConfig().getString("binaryPath").equals("")) {
            bsLocalArgs.put("binarypath", config.getConfig().getString("binaryPath"));
        }
        return bsLocalArgs;
    }

    public static void startLocal() throws Exception {
        long kickOutTime = System.currentTimeMillis() + 500000;
        do {
            // start browserStack local
            if(!browserStack.isRunning()) {
                browserStack.start(localArgs());
            }
        } while (!browserStack.isRunning() && System.currentTimeMillis() < kickOutTime);
        LOGGER.info("Is browserstack local enabled:" + browserStack.isRunning());
    }

    public static void stopLocal() throws Exception {
        browserStack.stop();
    }
}