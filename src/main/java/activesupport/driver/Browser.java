package activesupport.driver;

import activesupport.IllegalBrowserException;
import activesupport.config.Configuration;
import activesupport.driver.Parallel.*;
import activesupport.proxy.ProxyConfig;
import com.browserstack.local.Local;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Browser {

    private static WebDriver driver;

    private static String gridURL;
    private static String ipAddress;
    private static String portNumber;
    private static String platform;
    private static String browserVersion;
    protected static ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    private static final Logger LOGGER = LogManager.getLogger(Browser.class);

    static Local bsLocal = new Local();

    public static Configuration configuration = new Configuration();

    public static void setIpAddress(String ipAddress) {
        Browser.ipAddress = ipAddress;
    }

    public static void setPortNumber(String portNumber) {
        Browser.portNumber = portNumber;
    }

    public static String getIpAddress() {
        return ipAddress;
    }

    public static String getPortNumber() {
        return portNumber;
    }

    public static String getPlatform() {
        return platform;
    }

    public static void setPlatform(String platform) {
        Browser.platform = platform;
    }

    public static String getBrowserVersion() {
        return browserVersion;
    }

    public static void setBrowserVersion(String browserVersion) {
        Browser.browserVersion = browserVersion;
    }

    public static String getGridURL() {
        return gridURL;
    }

    public static void setGridURL(String gridURL) {
        Browser.gridURL = gridURL;
    }

    public static WebDriver navigate() {
        //set driver
        if (getDriver() == null) {
            setGridURL(System.getProperty("gridURL"));
            setPlatform(System.getProperty("platform"));
            setBrowserVersion(System.getProperty("browserVersion"));
            try {
                whichBrowser(System.getProperty("browser"));
            } catch (IllegalBrowserException | MalformedURLException e) {
                LOGGER.error("Error setting up browser: ", e);
            }
        }
        return getDriver();
    }

    public static WebDriver getDriver() {
        return threadLocalDriver.get();
    }

    public static String hubURL() {
        gridURL = gridURL == null ? "http://localhost:4444/wd/hub" : gridURL;
        return gridURL;
    }

    private static void whichBrowser(String browserName) throws IllegalBrowserException, MalformedURLException {
        browserName = browserName.toLowerCase().trim();
        ChromeSetUp chrome = new ChromeSetUp();
        FirefoxSetUp firefox = new FirefoxSetUp();
        EdgeSetUp edge = new EdgeSetUp();

        LOGGER.info("Setting up browser: " + browserName);

        switch (browserName) {
            case "chrome":
                driver = chrome.driver();
                break;
            case "edge":
                driver = edge.driver();
                break;
            case "firefox":
                driver = firefox.driver();
                break;
            case "safari":
                // Add Safari setup if needed
                break;
            case "headless":
                chrome.getChromeOptions().addArguments("--headless");
                driver = chrome.driver();
                break;
            case "chrome-proxy":
                chrome.getChromeOptions().setProxy(ProxyConfig.dvsaProxy().setSslProxy(getIpAddress().concat(":" + getPortNumber())));
                driver = chrome.driver();
                break;
            case "firefox-proxy":
                firefox.getOptions().setProxy(ProxyConfig.dvsaProxy().setSslProxy(getIpAddress().concat(":" + getPortNumber())));
                driver = firefox.driver();
                break;
            default:
                throw new IllegalBrowserException();
        }
        threadLocalDriver.set(driver);
        LOGGER.info("Browser setup complete: " + browserName);
    }

    public static void closeBrowser() throws Exception {
        if (getDriver() != null) {
            LOGGER.info("Closing browser");
            getDriver().quit();
        }
        bsLocal.stop();
        threadLocalDriver.remove();
        LOGGER.info("Browser closed and resources cleaned up");
    }

    public static boolean isBrowserOpen() {
        boolean isOpen = getDriver() != null;
        LOGGER.info("Checking if browser is open: " + isOpen);
        return isOpen;
    }
}