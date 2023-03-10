package activesupport.driver.Parallel;

import activesupport.proxy.ProxyConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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
        WebDriverManager.chromedriver().setup();
        chromeOptions.addArguments(ProxyConfig.ignoreCertErrors());
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--remote-allow-origins=*");
        if (getBrowserVersion() == null) {
            driver = new ChromeDriver(getChromeOptions());
        } else {
            chromeOptions.setCapability("proxy",ProxyConfig.dvsaProxy());
            chromeOptions.setCapability("browserstack.local", "true");
            chromeOptions.setCapability("browserstack.localIdentifier", "vol");
            chromeOptions.setCapability("browser_version", getBrowserVersion());
            chromeOptions.setCapability("platform", getPlatform());
            driver = new RemoteWebDriver(new URL(hubURL()), getChromeOptions());
        }
        return driver;
    }
}