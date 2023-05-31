package activesupport.driver.Parallel;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
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

    public WebDriver driver() throws MalformedURLException {
        WebDriverManager.chromedriver().setup();
        chromeOptions.setAcceptInsecureCerts(true);
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.setExperimentalOption("detach", false);
        if (getBrowserVersion() == null) {
            driver = new ChromeDriver(getChromeOptions());
        } else {
            chromeOptions.setPlatformName(getPlatform());
            driver = new RemoteWebDriver(new URL(hubURL()), getChromeOptions());
        }
        return driver;
    }
}