package activesupport.driver.Parallel;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

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
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--window-size=1920,1080");
        chromeOptions.addArguments("--hide-scrollbars");
        chromeOptions.addArguments("--force-device-scale-factor=1");
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