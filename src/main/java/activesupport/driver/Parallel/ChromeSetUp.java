package activesupport.driver.Parallel;

import activesupport.proxy.ProxyConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
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
        WebDriverManager.chromedriver().setup();
        chromeOptions.addArguments(ProxyConfig.ignoreCertErrors());
        chromeOptions.addArguments("--disable-dev-shm-usage");
        if (getBrowserVersion() == null) {
            driver = new ChromeDriver(getChromeOptions());
        } else {
            chromeOptions.setCapability("proxy",ProxyConfig.dvsaProxy());
            chromeOptions.setPlatformName(getPlatform());
            chromeOptions.setCapability("browser_version", getBrowserVersion());
            driver = new RemoteWebDriver(new URL(hubURL()), getChromeOptions());
        }
        return driver;
    }
}