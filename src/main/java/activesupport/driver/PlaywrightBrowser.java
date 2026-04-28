package activesupport.driver;

import com.microsoft.playwright.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class PlaywrightBrowser {

    private static final Logger LOGGER = LogManager.getLogger(PlaywrightBrowser.class);

    private Playwright playwright;
    private com.microsoft.playwright.Browser browser;
    private BrowserContext context;
    private Page page;

    public void create() {
        String browserName = System.getProperty("browser", "chrome");
        boolean headless = "true".equalsIgnoreCase(System.getProperty("headless", "true"));
        LOGGER.info("Creating Playwright session: browser={}, headless={}", browserName, headless);

        playwright = Playwright.create();

        BrowserType browserType = resolveBrowserType(browserName);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(launchArgs());

        if ("edge".equalsIgnoreCase(browserName)) {
            launchOptions.setChannel("msedge");
        }

        configureLaunchOptions(launchOptions);

        browser = browserType.launch(launchOptions);
        context = browser.newContext(contextOptions());
        page = context.newPage();
    }

    public Page page() {
        if (page == null) create();
        return page;
    }

    public BrowserContext context() {
        return context;
    }

    public boolean isOpen() {
        return page != null;
    }

    public byte[] screenshot() {
        return page != null ? page.screenshot() : new byte[0];
    }

    public void close() {
        safeClose(page, "page");
        page = null;
        safeClose(context, "context");
        context = null;
        safeClose(browser, "browser");
        browser = null;
        safeClose(playwright, "playwright");
        playwright = null;
    }

    protected BrowserType resolveBrowserType(String browserName) {
        return switch (normaliseBrowser(browserName)) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
    }

    protected List<String> launchArgs() {
        return List.of("--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage");
    }

    protected void configureLaunchOptions(BrowserType.LaunchOptions options) {
        // Override to customise launch options
    }

    protected com.microsoft.playwright.Browser.NewContextOptions contextOptions() {
        return new com.microsoft.playwright.Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true);
    }

    private String normaliseBrowser(String raw) {
        if (raw == null) return "chrome";
        return switch (raw.toLowerCase().trim()) {
            case "firefox", "firefox-proxy" -> "firefox";
            case "edge" -> "edge";
            case "webkit", "safari" -> "webkit";
            default -> "chrome";
        };
    }

    private void safeClose(AutoCloseable resource, String name) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing {}: {}", name, e.getMessage());
            }
        }
    }
}
