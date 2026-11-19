package activesupport.driver.Parallel;

import activesupport.proxy.JumpboxProxy;
import activesupport.system.Properties;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static activesupport.driver.Browser.*;

public class ChromeSetUp {

    private ChromeOptions chromeOptions = new ChromeOptions();

    public ChromeOptions getChromeOptions() {
        return chromeOptions;
    }

    public void setChromeOptions(ChromeOptions chromeOptions) {
        this.chromeOptions = chromeOptions;
    }

    public static WebDriver driver;

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
        if ("local".equalsIgnoreCase(Properties.get("env"))) {
            chromeOptions.setEnableDownloads(true);
            chromeOptions.addArguments("--disable-features=HttpsUpgrades,HttpsFirstBalancedModeAutoEnable");
            Map<String, Object> prefs = new HashMap<>();
            if (chromeOptions.getCapability(ChromeOptions.CAPABILITY) instanceof Map<?, ?> chromiumOptions
                    && chromiumOptions.get("prefs") instanceof Map<?, ?> existingPrefs) {
                existingPrefs.forEach((key, value) -> prefs.put(key.toString(), value));
            }
            prefs.put("https_only_mode_enabled", false);
            prefs.put("https_upgrades_enabled", false);
            chromeOptions.setExperimentalOption("prefs", prefs);
        }
        chromeOptions.setCapability("webSocketUrl", true);

        WebDriver driver;
        if (getBrowserVersion() == null) {
            JumpboxProxy.applyTo(chromeOptions);
            driver = new ChromeDriver(chromeOptions);
        } else {
            chromeOptions.setPlatformName(getPlatform());
            RemoteWebDriver remoteDriver = new RemoteWebDriver(new URL(hubURL()), chromeOptions);
            if ("local".equalsIgnoreCase(Properties.get("env"))) {
                remoteDriver.setFileDetector(new LocalFileDetector());
            }
            driver = remoteDriver;
        }
        return driver;
    }
}