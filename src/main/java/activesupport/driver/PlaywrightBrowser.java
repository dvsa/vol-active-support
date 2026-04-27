package activesupport.driver;

import com.microsoft.playwright.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Instance-based Playwright lifecycle manager.
 * <p>
 * Designed for composition — create one per test scenario (e.g. on a Cucumber World object)
 * and call {@link #close()} in the teardown hook. No ThreadLocal or static state.
 * <p>
 * Reads {@code -Dbrowser} and {@code -Dheadless} system properties.
 */
public class PlaywrightBrowser {

    private static final Logger LOGGER = LogManager.getLogger(PlaywrightBrowser.class);

    private Playwright playwright;
    private com.microsoft.playwright.Browser browser;
    private BrowserContext context;
    private Page page;

    /**
     * Creates the Playwright browser, context and page.
     * Call once at the start of a test scenario.
     */
    public void create() {
        String browserName = System.getProperty("browser", "chrome");
        boolean headless = isHeadless();
        LOGGER.info("Creating Playwright session: browser={}, headless={}", browserName, headless);

        playwright = Playwright.create();

        BrowserType browserType = switch (normaliseBrowser(browserName)) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };

        List<String> args = Arrays.asList("--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage");
        BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(args);
        if ("edge".equalsIgnoreCase(browserName)) {
            opts.setChannel("msedge");
        }

        browser = browserType.launch(opts);
        context = browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true));
        page = context.newPage();
    }

    /** Returns the current page, lazily creating the session if needed. */
    public Page page() {
        if (page == null) create();
        return page;
    }

    public BrowserContext context() { return context; }

    public boolean isOpen() { return page != null; }

    /** Takes a full-page screenshot, or returns an empty byte array if no page is open. */
    public byte[] screenshot() {
        return page != null ? page.screenshot() : new byte[0];
    }

    /**
     * Tears down page → context → browser → playwright in order.
     * Safe to call multiple times.
     */
    public void close() {
        try { if (page != null) page.close(); } catch (Exception e) { LOGGER.warn("Error closing page: {}", e.getMessage()); } finally { page = null; }
        try { if (context != null) context.close(); } catch (Exception e) { LOGGER.warn("Error closing context: {}", e.getMessage()); } finally { context = null; }
        try { if (browser != null) browser.close(); } catch (Exception e) { LOGGER.warn("Error closing browser: {}", e.getMessage()); } finally { browser = null; }
        try { if (playwright != null) playwright.close(); } catch (Exception e) { LOGGER.warn("Error closing playwright: {}", e.getMessage()); } finally { playwright = null; }
    }

    private boolean isHeadless() {
        return "true".equalsIgnoreCase(System.getProperty("headless", "true"));
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
}
