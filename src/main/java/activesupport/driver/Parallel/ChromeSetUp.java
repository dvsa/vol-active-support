package activesupport.driver.Parallel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static activesupport.driver.Browser.*;

public class ChromeSetUp {

    private static final Logger LOGGER = LogManager.getLogger(ChromeSetUp.class);

    private ChromeOptions chromeOptions = new ChromeOptions();

    public ChromeOptions getChromeOptions() {
        return chromeOptions;
    }

    public void setChromeOptions(ChromeOptions chromeOptions) {
        this.chromeOptions = chromeOptions;
    }

    public static WebDriver driver;

    /**
     * Network logging is on by default so a failing run captures the trace without a rerun.
     */
    public static boolean networkLoggingEnabled() {
        return Boolean.parseBoolean(System.getProperty("networkLogging", "true"));
    }

    public WebDriver driver() throws MalformedURLException {
        chromeOptions.setAcceptInsecureCerts(true);
        if (isHeadless()) {
            chromeOptions.addArguments("--headless=new");
        }
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--window-size=1920,1080");
        chromeOptions.addArguments("--hide-scrollbars");
        chromeOptions.addArguments("--force-device-scale-factor=1");

        if (networkLoggingEnabled()) {
            LoggingPreferences loggingPreferences = new LoggingPreferences();
            loggingPreferences.enable(LogType.PERFORMANCE, Level.ALL);
            loggingPreferences.enable(LogType.BROWSER, Level.ALL);
            chromeOptions.setCapability(ChromeOptions.LOGGING_PREFS, loggingPreferences);

            Map<String, Object> perfLoggingPrefs = new HashMap<>();
            perfLoggingPrefs.put("enableNetwork", true);
            perfLoggingPrefs.put("enablePage", true);
            perfLoggingPrefs.put("traceCategories", "devtools.network");
            chromeOptions.setExperimentalOption("perfLoggingPrefs", perfLoggingPrefs);

            LOGGER.info("Chrome network logging ENABLED - use NetworkDiagnostics.reportAll(driver, context) "
                    + "to dump the trace. Disable with -DnetworkLogging=false");
        }

        chromeOptions.setCapability("webSocketUrl", true);

        WebDriver driver;
        if (getBrowserVersion() == null) {
            driver = new ChromeDriver(chromeOptions);
        } else {
            chromeOptions.setPlatformName(getPlatform());
            driver = new RemoteWebDriver(new URL(hubURL()), chromeOptions);
        }
        return driver;
    }
}