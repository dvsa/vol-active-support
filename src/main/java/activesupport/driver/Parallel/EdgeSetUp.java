package activesupport.driver.Parallel;

import activesupport.proxy.ProxyConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.net.MalformedURLException;
import java.net.URL;
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

    public WebDriver driver() throws MalformedURLException {
        edgeOptions.setAcceptInsecureCerts(true);
        edgeOptions.addArguments("--headless=new");
        edgeOptions.addArguments("--no-sandbox");
        edgeOptions.addArguments("--disable-gpu");
        edgeOptions.addArguments("--disable-dev-shm-usage");
        edgeOptions.addArguments("--window-size=1920,1080");
        edgeOptions.addArguments("--hide-scrollbars");
        edgeOptions.addArguments("--force-device-scale-factor=1");
        edgeOptions.setCapability("webSocketUrl", true);

        WebDriver driver;
        if (getBrowserVersion() == null) {
            driver = new EdgeDriver(edgeOptions);
        } else {
            edgeOptions.setPlatformName(getPlatform());
            driver = new RemoteWebDriver(new URL(hubURL()), edgeOptions);
        }
        return driver;
    }
}