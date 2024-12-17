package activesupport.driver;

import activesupport.IllegalBrowserException;
import activesupport.config.Configuration;
import activesupport.driver.Parallel.*;
import activesupport.proxy.ProxyConfig;
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
    protected static
    ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    private static final Logger LOGGER = LogManager.getLogger(Browser.class);


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
                LOGGER.info("STACK TRACE: ".concat(e.toString()));
            }
        }
        return getDriver();
    }

    public static WebDriver getDriver(){
        return threadLocalDriver.get();
    }

    public static String hubURL(){
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
                chrome.getChromeOptions().setProxy(ProxyConfig.dvsaProxy().setSslProxy(getIpAddress().concat(":"+getPortNumber())));
                driver = chrome.driver();
                break;
            case "firefox-proxy":
                firefox.getOptions().setProxy(ProxyConfig.dvsaProxy().setSslProxy(getIpAddress().concat(":"+getPortNumber())));
                driver = firefox.driver();
                break;
            default:
                throw new IllegalBrowserException();
        }
        threadLocalDriver.set(driver);
        getDriver();
    }

    public static void closeBrowser() throws Exception {
        if (getDriver() != null)
            getDriver().quit();
    }

    public static boolean isBrowserOpen() {
        boolean isOpen;
        isOpen = getDriver() != null;
        return isOpen;
    }

    public static void removeLocalDriverThread() {
        threadLocalDriver.remove();
    }
}
