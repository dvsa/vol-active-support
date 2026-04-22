package activesupport.driver;

import com.microsoft.playwright.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Playwright browser manager, parallel to the Selenium {@link Browser} class.
 * <p>
 * Each thread gets its own Playwright instance, Browser, BrowserContext, and Page
 * (Playwright is NOT thread-safe, so nothing is shared across threads).
 * <p>
 * Activate by passing {@code -Dautomation.engine=playwright} to Maven.
 */
public class PlaywrightBrowser {

    private static final Logger LOGGER = LogManager.getLogger(PlaywrightBrowser.class);

    private static final ThreadLocal<Playwright> threadPlaywright = new ThreadLocal<>();
    private static final ThreadLocal<com.microsoft.playwright.Browser> threadBrowser = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> threadContext = new ThreadLocal<>();
    private static final ThreadLocal<Page> threadPage = new ThreadLocal<>();

    public static boolean isPlaywright() {
        return "playwright".equalsIgnoreCase(System.getProperty("automation.engine"));
    }

    /**
     * Returns the current thread's Playwright {@link Page}, creating one if needed.
     * Analogous to {@link Browser#navigate()}.
     */
    public static Page navigate() {
        if (threadPage.get() == null) {
            String browserName = System.getProperty("browser", "chrome");
            boolean headless = isHeadless(browserName);
            launchBrowser(normaliseBrowserName(browserName), headless);
        }
        return threadPage.get();
    }

    public static Page getPage() {
        return threadPage.get();
    }

    public static BrowserContext getContext() {
        return threadContext.get();
    }

    public static boolean isBrowserOpen() {
        return threadPage.get() != null;
    }

    public static void closeBrowser() {
        try {
            Page page = threadPage.get();
            if (page != null) {
                page.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing Playwright page: {}", e.getMessage());
        } finally {
            threadPage.remove();
        }

        try {
            BrowserContext ctx = threadContext.get();
            if (ctx != null) {
                ctx.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing Playwright context: {}", e.getMessage());
        } finally {
            threadContext.remove();
        }

        try {
            com.microsoft.playwright.Browser browser = threadBrowser.get();
            if (browser != null) {
                browser.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing Playwright browser: {}", e.getMessage());
        } finally {
            threadBrowser.remove();
        }

        try {
            Playwright pw = threadPlaywright.get();
            if (pw != null) {
                pw.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing Playwright instance: {}", e.getMessage());
        } finally {
            threadPlaywright.remove();
        }
    }

    private static void launchBrowser(String browserType, boolean headless) {
        LOGGER.info("Launching Playwright {} (headless={})", browserType, headless);

        Playwright pw = Playwright.create();
        threadPlaywright.set(pw);

        BrowserType type = switch (browserType) {
            case "firefox" -> pw.firefox();
            case "webkit" -> pw.webkit();
            default -> pw.chromium();
        };

        List<String> defaultArgs = Arrays.asList(
                "--no-sandbox",
                "--disable-gpu",
                "--disable-dev-shm-usage"
        );

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(defaultArgs);

        // Edge channel support
        if ("edge".equals(browserType)) {
            launchOptions.setChannel("msedge");
        }

        com.microsoft.playwright.Browser browser = type.launch(launchOptions);
        threadBrowser.set(browser);

        com.microsoft.playwright.Browser.NewContextOptions contextOptions =
                new com.microsoft.playwright.Browser.NewContextOptions()
                        .setViewportSize(1920, 1080)
                        .setIgnoreHTTPSErrors(true);

        BrowserContext context = browser.newContext(contextOptions);
        threadContext.set(context);

        Page page = context.newPage();
        threadPage.set(page);
    }

    private static String normaliseBrowserName(String raw) {
        if (raw == null) return "chrome";
        return switch (raw.toLowerCase().trim()) {
            case "firefox", "firefox-proxy" -> "firefox";
            case "edge" -> "edge";
            case "webkit", "safari" -> "webkit";
            default -> "chrome"; // chrome, headless, chrome-proxy
        };
    }

    private static boolean isHeadless(String raw) {
        if (raw == null) return true;
        String name = raw.toLowerCase().trim();
        // Default to headless unless explicitly running headed
        String headlessProperty = System.getProperty("headless", "true");
        return "headless".equals(name) || "true".equalsIgnoreCase(headlessProperty);
    }
}
