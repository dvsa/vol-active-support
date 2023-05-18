package activesupport.driver.Parallel;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

    public static List<String> arguments() {
        List<String> chromeSwitches = new ArrayList<>();
        chromeSwitches.add("--ignore-certificate-errors");
        chromeSwitches.add("--allow-running-insecure-content");
        chromeSwitches.add("--disable-gpu");
        chromeSwitches.add("--disable-dev-shm-usage");
        return chromeSwitches;
    }
    public WebDriver driver() throws MalformedURLException {
        WebDriverManager.chromedriver().setup();
        chromeOptions.setAcceptInsecureCerts(true);
        chromeOptions.addArguments(arguments());
        if (getBrowserVersion() == null) {
            driver = new ChromeDriver(getChromeOptions());
        } else {
            chromeOptions.setPlatformName(getPlatform());
            chromeOptions.setCapability("browser_version", getBrowserVersion());
            driver = new RemoteWebDriver(new URL(hubURL()), getChromeOptions());
        }
        return driver;
    }
}