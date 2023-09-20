package activesupport.driver.Parallel;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

import static activesupport.driver.Browser.*;

public class FirefoxSetUp {


    private FirefoxOptions options = new FirefoxOptions();

    public FirefoxOptions getOptions() {
        return options;
    }

    public void setOptions(FirefoxOptions options) {
        this.options = options;
    }

    public static WebDriver driver;

    public WebDriver driver() throws MalformedURLException {
        options.setAcceptInsecureCerts(true);
        if (getPlatform() == null) {
            driver = new FirefoxDriver(getOptions());
        } else {
            options.setPlatformName(getPlatform());
            driver = new RemoteWebDriver(new URL(hubURL()), getOptions());
        }
        return driver;
    }
}