package activesupport.driver.Parallel;

import activesupport.proxy.ProxyConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static activesupport.driver.Browser.*;

public class EdgeSetUp {
    private static final Logger LOGGER = LogManager.getLogger(EdgeSetUp.class);
    private EdgeOptions edgeOptions = new EdgeOptions();

    public EdgeOptions getEdgeOptions() {
        return edgeOptions;
    }

    public void setEdgeOptions(EdgeOptions edgeOptions) {
        this.edgeOptions = edgeOptions;
    }

    public static WebDriver driver;

    public WebDriver driver() throws MalformedURLException {
        WebDriverManager.edgedriver().setup();
        if (getBrowserVersion() == null) {
            driver = new EdgeDriver(edgeOptions);
        } else {
            edgeOptions.setCapability("proxy",ProxyConfig.dvsaProxy());
            edgeOptions.setCapability("browserstack.local", "true");
            edgeOptions.setCapability("browserstack.localIdentifier", "vol");
            edgeOptions.setCapability("browser_version", getBrowserVersion());
            edgeOptions.setCapability("platform", getPlatform());
            driver = new RemoteWebDriver(new URL(hubURL()), edgeOptions);
        }
        return driver;
    }
}