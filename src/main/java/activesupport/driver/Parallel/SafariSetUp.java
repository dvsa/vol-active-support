package activesupport.driver.Parallel;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.net.MalformedURLException;
import java.net.URL;

import static activesupport.driver.Browser.*;

public class SafariSetUp {

    private SafariOptions safari = new SafariOptions();

    public SafariOptions getSafari() {
        return safari;
    }


    public void setSafari(SafariOptions safari) {
        this.safari = safari;
    }

    public static WebDriver driver;

    public WebDriver driver() throws MalformedURLException {
        WebDriverManager.safaridriver().setup();
        safari.setCapability("idleTimeout", 5000);
        safari.setCapability("name", "VOL-Cross-Browser");
        if (getPlatform() == null) {
            driver = new SafariDriver(getSafari());
        } else {
            safari.setCapability("platform", getPlatform());
            safari.setCapability("browser_version", getBrowserVersion());
            safari.setCapability("browserstack.localIdentifier", "vol");
            safari.setCapability("browserstack.local", "true");
            driver = new RemoteWebDriver(new URL(hubURL()), getSafari()
            );
        }
        return driver;
    }
}