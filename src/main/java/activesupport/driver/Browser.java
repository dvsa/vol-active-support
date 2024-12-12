package activesupport.driver;

import activesupport.IllegalBrowserException;
import activesupport.config.Configuration;
import activesupport.driver.Parallel.*;
import activesupport.proxy.ProxyConfig;
import com.browserstack.local.Local;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import java.net.URL;
import java.net.MalformedURLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Browser {
    private static final Logger LOGGER = LogManager.getLogger(Browser.class);
    private static WebDriver driver;
    private static String gridURL;
    private static String ipAddress;
    private static String portNumber;
    private static String platform;
    private static String browserVersion;
    protected static ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    static Local bsLocal = new Local();
    public static Configuration configuration = new Configuration();

    public static void setIpAddress(String ipAddress) {
        LOGGER.info("Setting IP address to: {}", ipAddress);
        Browser.ipAddress = ipAddress;
    }

    public static void setPortNumber(String portNumber) {
        LOGGER.info("Setting port number to: {}", portNumber);
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
        LOGGER.info("Setting platform to: {}", platform);
        Browser.platform = platform;
    }

    public static String getBrowserVersion() {
        return browserVersion;
    }

    public static void setBrowserVersion(String browserVersion) {
        LOGGER.info("Setting browser version to: {}", browserVersion);
        Browser.browserVersion = browserVersion;
    }

    public static String getGridURL() {
        return gridURL;
    }

    public static void setGridURL(String gridURL) {
        LOGGER.info("Setting grid URL to: {}", gridURL);
        Browser.gridURL = gridURL;
    }

    public static WebDriver navigate() {
        LOGGER.info("Starting browser navigation...");
        if (getDriver() == null) {
            LOGGER.info("No existing driver found, initializing new session");
            setGridURL(System.getProperty("gridURL"));
            setPlatform(System.getProperty("platform"));
            setBrowserVersion(System.getProperty("browserVersion"));

            try {
                LOGGER.info("Attempting to initialize browser with properties:");
                LOGGER.info("Grid URL: {}", getGridURL());
                LOGGER.info("Platform: {}", getPlatform());
                LOGGER.info("Browser Version: {}", getBrowserVersion());
                whichBrowser(System.getProperty("browser"));
            } catch (IllegalBrowserException | MalformedURLException e) {
                LOGGER.error("Failed to initialize browser: {}", e.getMessage());
                LOGGER.error("Stack trace: ", e);
                throw new RuntimeException("Failed to initialize browser", e);
            }
        } else {
            LOGGER.info("Using existing WebDriver instance");
        }
        return getDriver();
    }

    public static WebDriver getDriver() {
        WebDriver currentDriver = threadLocalDriver.get();
        if (currentDriver != null) {
            try {
                if (currentDriver instanceof RemoteWebDriver) {
                    SessionId sessionId = ((RemoteWebDriver) currentDriver).getSessionId();
                    LOGGER.debug("Current session ID: {}", sessionId);
                }
                currentDriver.getCurrentUrl();
                LOGGER.debug("Driver health check passed");
            } catch (Exception e) {
                LOGGER.warn("Detected dead session, cleaning up. Error: {}", e.getMessage());
                closeBrowser();
                return null;
            }
        } else {
            LOGGER.debug("No driver found in ThreadLocal");
        }
        return currentDriver;
    }

    public static String hubURL() {
        String url = gridURL == null ? "http://localhost:4444/wd/hub" : gridURL;
        LOGGER.info("Using Hub URL: {}", url);
        return url;
    }

    private static void whichBrowser(String browserName) throws IllegalBrowserException, MalformedURLException {
        if (browserName == null) {
            LOGGER.error("Browser name is null");
            throw new IllegalBrowserException("Browser name cannot be null");
        }

        browserName = browserName.toLowerCase().trim();
        LOGGER.info("Initializing {} browser", browserName);

        ChromeSetUp chrome = new ChromeSetUp();
        FirefoxSetUp firefox = new FirefoxSetUp();
        EdgeSetUp edge = new EdgeSetUp();

        try {
            switch (browserName) {
                case "chrome":
                    LOGGER.info("Setting up Chrome browser");
                    driver = chrome.driver();
                    break;
                case "edge":
                    LOGGER.info("Setting up Edge browser");
                    driver = edge.driver();
                    break;
                case "firefox":
                    LOGGER.info("Setting up Firefox browser");
                    driver = firefox.driver();
                    break;
                case "safari":
                    LOGGER.error("Safari browser is not implemented");
                    throw new IllegalBrowserException("Safari is not implemented");
                case "headless":
                    LOGGER.info("Setting up headless Chrome browser");
                    chrome.getChromeOptions().addArguments("--headless");
                    driver = chrome.driver();
                    break;
                case "chrome-proxy":
                    LOGGER.info("Setting up Chrome browser with proxy");
                    LOGGER.info("Proxy configuration - IP: {}, Port: {}", getIpAddress(), getPortNumber());
                    chrome.getChromeOptions().setProxy(ProxyConfig.dvsaProxy()
                            .setSslProxy(getIpAddress() + ":" + getPortNumber()));
                    driver = chrome.driver();
                    break;
                case "firefox-proxy":
                    LOGGER.info("Setting up Firefox browser with proxy");
                    LOGGER.info("Proxy configuration - IP: {}, Port: {}", getIpAddress(), getPortNumber());
                    firefox.getOptions().setProxy(ProxyConfig.dvsaProxy()
                            .setSslProxy(getIpAddress() + ":" + getPortNumber()));
                    driver = firefox.driver();
                    break;
                default:
                    LOGGER.error("Unsupported browser type: {}", browserName);
                    throw new IllegalBrowserException("Unsupported browser: " + browserName);
            }

            if (driver instanceof RemoteWebDriver) {
                SessionId sessionId = ((RemoteWebDriver) driver).getSessionId();
                LOGGER.info("Successfully created new session with ID: {}", sessionId);
                LOGGER.info("Browser capabilities: {}", ((RemoteWebDriver) driver).getCapabilities().toString());
            }

            threadLocalDriver.set(driver);
            LOGGER.info("Successfully initialized {} browser", browserName);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize {} browser", browserName);
            LOGGER.error("Error details: {}", e.getMessage());
            LOGGER.error("Stack trace: ", e);
            throw e;
        }
    }

    public static void closeBrowser() {
        WebDriver currentDriver = threadLocalDriver.get();
        LOGGER.info("Attempting to close browser");

        try {
            if (currentDriver != null) {
                if (currentDriver instanceof RemoteWebDriver) {
                    SessionId sessionId = ((RemoteWebDriver) currentDriver).getSessionId();
                    LOGGER.info("Closing session: {}", sessionId);
                }
                currentDriver.quit();
                LOGGER.info("Browser closed successfully");
            } else {
                LOGGER.info("No active browser session to close");
            }

            bsLocal.stop();
            LOGGER.info("BrowserStack local connection stopped");
        } catch (Exception e) {
            LOGGER.error("Error while closing browser: {}", e.getMessage());
            LOGGER.error("Stack trace: ", e);
        } finally {
            threadLocalDriver.remove();
            LOGGER.info("ThreadLocal driver removed");
        }
    }

    public static boolean isBrowserOpen() {
        WebDriver currentDriver = getDriver();
        boolean isOpen = false;

        if (currentDriver != null) {
            try {
                currentDriver.getCurrentUrl();
                isOpen = true;
                if (currentDriver instanceof RemoteWebDriver) {
                    SessionId sessionId = ((RemoteWebDriver) currentDriver).getSessionId();
                    LOGGER.debug("Browser session {} is active", sessionId);
                }
            } catch (Exception e) {
                LOGGER.warn("Browser session appears to be dead: {}", e.getMessage());
                isOpen = false;
            }
        } else {
            LOGGER.debug("No active browser session found");
        }

        LOGGER.info("Browser status check - Is Open: {}", isOpen);
        return isOpen;
    }
}