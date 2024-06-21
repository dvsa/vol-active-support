package activesupport.driver;

import activesupport.IllegalBrowserException;
import activesupport.config.Configuration;
import activesupport.driver.Parallel.*;
import activesupport.proxy.ProxyConfig;
import com.browserstack.local.Local;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.MalformedURLException;
import java.net.URL;

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
        // Set driver
        if (getDriver() == null) {
            setGridURL(System.getProperty("gridURL"));
            setPlatform(System.getProperty("platform"));
            setBrowserVersion(System.getProperty("browserVersion"));
            try {
                whichBrowser(System.getProperty("browser"));
            } catch (IllegalBrowserException | MalformedURLException e) {
                LOGGER.error("STACK TRACE: ".concat(e.toString()));
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
                break;
            case "headless":
                chrome.getChromeOptions().addArguments("--headless");
                driver = chrome.driver();
                break;
            case "chrome-proxy":
                chrome.getChromeOptions().setProxy(ProxyConfig.dvsaProxy().setSslProxy(getIpAddress().concat(":").concat(getPortNumber())));
                driver = chrome.driver();
                break;
            case "firefox-proxy":
                firefox.getOptions().setProxy(ProxyConfig.dvsaProxy().setSslProxy(getIpAddress().concat(":").concat(getPortNumber())));
                driver = firefox.driver();
                break;
            default:
                throw new IllegalBrowserException();
        }
        threadLocalDriver.set(driver);
    }

    public static void closeBrowser() {
        if (getDriver() != null) {
            getDriver().quit();
        }
        try {
            bsLocal.stop();
        } catch (Exception e) {
            LOGGER.error("Error stopping BrowserStack local: ", e);
        }
        threadLocalDriver.remove();
    }

    public static boolean isBrowserOpen() {
        return getDriver() != null;
    }

    private static boolean isSessionValid(WebDriver driver) {
        try {
            if (driver != null) {
                ((RemoteWebDriver) driver).getSessionId();
                return true;
            }
        } catch (NoSuchSessionException e) {
            LOGGER.warn("Session is invalid: " + e.getMessage());
        }
        return false;
    }

    public static void ensureSession() {
        if (!isSessionValid(getDriver())) {
            LOGGER.info("Creating a new session as the current session is invalid or does not exist.");
            try {
                whichBrowser(System.getProperty("browser"));
            } catch (IllegalBrowserException | MalformedURLException e) {
                LOGGER.error("Error while creating a new session: ", e);
            }
        }
    }

    public static void deleteAllCookies() {
        ensureSession();
        try {
            getDriver().manage().deleteAllCookies();
        } catch (NoSuchSessionException e) {
            LOGGER.error("Session not found while deleting cookies, creating a new session.", e);
            ensureSession();
            getDriver().manage().deleteAllCookies();
        }
    }
}