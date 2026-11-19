package activesupport.driver.Parallel;

import activesupport.proxy.JumpboxProxy;
import activesupport.proxy.ProxyConfig;
import activesupport.system.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
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

    public WebDriver driver() throws MalformedURLException {
        edgeOptions.setAcceptInsecureCerts(true);
        if (isHeadless()) {
            edgeOptions.addArguments("--headless=new");
        }
        edgeOptions.addArguments("--no-sandbox");
        edgeOptions.addArguments("--disable-gpu");
        edgeOptions.addArguments("--disable-dev-shm-usage");
        edgeOptions.addArguments("--window-size=1920,1080");
        edgeOptions.addArguments("--hide-scrollbars");
        edgeOptions.addArguments("--force-device-scale-factor=1");

        boolean local = "local".equalsIgnoreCase(Properties.get("env"));
        edgeOptions.addArguments("--disable-features=AcceptCHFrame,BackForwardCache,Prerender2," +
                "PreconnectToSearch,PrefetchProxy,OptimizationHints,Translate,MediaRouter," +
                "AutofillServerCommunication,InterestFeedContentSuggestions,CalculateNativeWinOcclusion" +
                (local ? ",HttpsUpgrades,HttpsFirstBalancedModeAutoEnable" : ""));
        edgeOptions.addArguments("--disable-back-forward-cache");
        edgeOptions.addArguments("--disable-quic");
        edgeOptions.addArguments("--disable-http2");
        edgeOptions.addArguments("--disable-auto-reload");
        edgeOptions.addArguments("--dns-prefetch-disable");
        edgeOptions.addArguments("--disable-background-networking");
        edgeOptions.addArguments("--disable-client-side-phishing-detection");
        edgeOptions.addArguments("--disable-domain-reliability");
        edgeOptions.addArguments("--disable-sync");
        edgeOptions.addArguments("--safebrowsing-disable-auto-update");
        edgeOptions.addArguments("--no-first-run");
        edgeOptions.addArguments("--no-default-browser-check");
        edgeOptions.addArguments("--disable-search-engine-choice-screen");
        edgeOptions.addArguments("--disable-popup-blocking");
        edgeOptions.setAcceptInsecureCerts(true);
        edgeOptions.setEnableDownloads(true);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("net.network_prediction_options", 2);
        prefs.put("safebrowsing.enabled", false);
        if (local) {
            prefs.put("https_only_mode_enabled", false);
            prefs.put("https_upgrades_enabled", false);
        }
        edgeOptions.setExperimentalOption("prefs", prefs);

        edgeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        edgeOptions.setCapability("webSocketUrl", true);

        WebDriver driver;
        if (getBrowserVersion() == null) {
            JumpboxProxy.applyTo(edgeOptions);
            driver = new EdgeDriver(edgeOptions);
        } else {
            edgeOptions.setPlatformName(getPlatform());
            RemoteWebDriver remoteDriver = new RemoteWebDriver(new URL(hubURL()), edgeOptions);
            if (local) {
                remoteDriver.setFileDetector(new LocalFileDetector());
            }
            driver = remoteDriver;
        }
        return driver;
    }
}