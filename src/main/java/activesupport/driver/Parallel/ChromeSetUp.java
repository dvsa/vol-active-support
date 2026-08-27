package activesupport.driver.Parallel;

import org.openqa.selenium.PageLoadStrategy;
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
        chromeOptions.setAcceptInsecureCerts(true);
        chromeOptions.addArguments("--headless=new");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--window-size=1920,1080");
        chromeOptions.addArguments("--hide-scrollbars");
        chromeOptions.addArguments("--force-device-scale-factor=1");

        // --- Prevent Chrome re-issuing a navigation request (duplicate OAuth ?code= callbacks) ---
        // A single-use authorisation code must only ever reach the app once; any Chrome-initiated



        chromeOptions.addArguments("--disable-features=AcceptCHFrame,BackForwardCache,Prerender2," +
                "PreconnectToSearch,PrefetchProxy,OptimizationHints,Translate,MediaRouter," +
                "AutofillServerCommunication,InterestFeedContentSuggestions,CalculateNativeWinOcclusion");
        chromeOptions.addArguments("--disable-back-forward-cache");

        // HTTP/2 GOAWAY and QUIC connection migration both cause Chrome to transparently replay
        // idempotent GETs on a new connection. Under Grid concurrency the LB will do this.
        chromeOptions.addArguments("--disable-quic");
        chromeOptions.addArguments("--disable-http2");

        // Chrome auto-reloads error pages (a transient 5xx/timeout under load would re-send the code).
        chromeOptions.addArguments("--disable-auto-reload");

        // Speculative connections/prefetching and background chatter add noise and extra requests.
        chromeOptions.addArguments("--dns-prefetch-disable");
        chromeOptions.addArguments("--disable-background-networking");
        chromeOptions.addArguments("--disable-client-side-phishing-detection");
        chromeOptions.addArguments("--disable-domain-reliability");
        chromeOptions.addArguments("--disable-sync");
        chromeOptions.addArguments("--safebrowsing-disable-auto-update");
        chromeOptions.addArguments("--no-first-run");
        chromeOptions.addArguments("--no-default-browser-check");
        chromeOptions.addArguments("--disable-search-engine-choice-screen");
        chromeOptions.addArguments("--disable-popup-blocking");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("net.network_prediction_options", 2); // NETWORK_PREDICTION_NEVER
        prefs.put("safebrowsing.enabled", false);
        chromeOptions.setExperimentalOption("prefs", prefs);

        // Wait for the full load before the test proceeds, so nothing navigates over an in-flight
        // callback request.
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);

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