package activesupport.visual;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visual regression capture and triage, driven by {@code -DvisualTriage.enabled=true}.
 *
 * <p>Call {@link #capture(WebDriver)} on each page (e.g. from a Cucumber {@code @AfterStep} hook) and
 * {@link #generateReport()} once at the end of the run. Each environment's pages are saved under
 * {@code visual-snapshots/<env>/} keyed by an environment-neutral path, so a run against the baseline
 * environment (default {@code dev}) followed by a run against another environment produces a comparison
 * report, triaged by the Bedrock agent, in {@code target/reports/}.</p>
 */
public final class VisualCapture {

    private static final Logger LOGGER = LogManager.getLogger(VisualCapture.class);
    private static final String PREFIX = "visualTriage.";
    private static final Set<String> CAPTURED = ConcurrentHashMap.newKeySet();
    private static final String DEFAULT_MASK = ".govuk-cookie-banner,.cookie-banner,[class*='version-header'],"
            + "[class*='environment-banner'],.govuk-header__navigation-item--user,[data-module='session-timeout']";

    private static final ThreadLocal<String> SCOPE = ThreadLocal.withInitial(() -> "");

    private VisualCapture() {
    }

    /**
     * Prefixes subsequent page keys on this thread (e.g. with the scenario name) so each environment's page is
     * compared with the same page reached through the same journey and data state. Blank clears the scope.
     */
    public static void setScope(String name) {
        String slug = name == null ? "" : name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        SCOPE.set(slug.length() > 60 ? slug.substring(0, 60) : slug);
    }

    public static boolean isEnabled() {
        return VisualTriageConfig.isEnabled();
    }

    public static String environment() {
        return System.getProperty("env", "unknown").toLowerCase().trim();
    }

    public static String baselineEnvironment() {
        return System.getProperty(PREFIX + "baselineEnv", "dev").toLowerCase().trim();
    }

    public static Path snapshotRoot() {
        return Paths.get(System.getProperty(PREFIX + "dir", "visual-snapshots"));
    }

    /** Captures the current page once per run; never throws, so it is safe to call from hooks. */
    public static void capture(WebDriver driver) {
        if (!isEnabled() || driver == null) {
            return;
        }
        try {
            String url = driver.getCurrentUrl();
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String key = pageKey(url, documentLanguage(js));
            if (key == null || !CAPTURED.add(key)) {
                return;
            }
            Path png = snapshotRoot().resolve(environment()).resolve(key + ".png");
            Files.createDirectories(png.getParent());
            js.executeScript("""
                    const s = document.createElement('style'); s.id = '__visual_mask';
                    s.textContent = arguments[0] + '{visibility:hidden !important}'
                      + '*{caret-color:transparent !important;transition:none !important;animation:none !important}';
                    document.head.appendChild(s);""", System.getProperty(PREFIX + "mask", DEFAULT_MASK));
            try {
                Files.write(png, screenshot(driver));
                PageGeometry.captureTo(driver, png.resolveSibling(png.getFileName().toString().replace(".png", ".json")));
            } finally {
                js.executeScript("const s = document.getElementById('__visual_mask'); if (s) s.remove();");
            }
            LOGGER.info("VISUAL-TRIAGE: captured {} -> {}", url, png);
        } catch (Exception e) {
            LOGGER.warn("VISUAL-TRIAGE: capture failed: {}", e.getMessage());
        }
    }

    private static byte[] screenshot(WebDriver driver) {
        if (driver instanceof HasCdp cdp) {
            try {
                Map<String, Object> result = cdp.executeCdpCommand("Page.captureScreenshot",
                        Map.of("format", "png", "captureBeyondViewport", true, "fromSurface", true));
                return Base64.getDecoder().decode((String) result.get("data"));
            } catch (Exception e) {
                LOGGER.debug("VISUAL-TRIAGE: full-page capture unavailable, using viewport: {}", e.getMessage());
            }
        }
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    /**
     * Environment-neutral file key: host removed, numeric path segments replaced by {@code id}, only the
     * {@code lang} query kept. e.g. {@code https://ssweb.reg.../application/123/vehicles/?lang=cy}
     * becomes {@code application/id/vehicles/index-cy}.
     */
    public static String pageKey(String url) {
        return pageKey(url, null);
    }

    /** Whether a page with this key has already been captured in this run. */
    public static boolean isCaptured(String key) {
        return key != null && CAPTURED.contains(key);
    }

    /** The rendered language, so Welsh chosen by cookie is not saved over the English page. */
    private static String documentLanguage(JavascriptExecutor js) {
        try {
            Object lang = js.executeScript("return document.documentElement.lang || ''");
            String value = lang == null ? "" : lang.toString().toLowerCase().replaceAll("[^a-z].*", "");
            return value.isEmpty() ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    public static String pageKey(String url, String documentLang) {
        if (url == null || !url.startsWith("http")) {
            return null;
        }
        URI uri = URI.create(url);
        String host = uri.getHost() == null ? "" : uri.getHost();
        String app = host.startsWith("ss") || host.contains("selfserve") ? "selfserve" : "internal";
        String scope = SCOPE.get();
        StringBuilder key = new StringBuilder(scope.isEmpty() ? "" : scope + "/").append(app);
        for (String segment : (uri.getPath() == null ? "" : uri.getPath()).split("/")) {
            if (segment.isBlank()) {
                continue;
            }
            key.append('/').append(segment.matches("\\d+|[A-Z]{1,2}\\d{5,}") ? "id"
                    : segment.replaceAll("[^A-Za-z0-9._-]", "_"));
        }
        String lang = documentLang == null ? "en" : documentLang;
        if (documentLang == null && uri.getQuery() != null) {
            for (String pair : uri.getQuery().split("&")) {
                if (pair.startsWith("lang=")) {
                    lang = pair.substring(5);
                }
            }
        }
        return key.append("/index-").append(lang.replaceAll("[^A-Za-z]", "")).toString();
    }

    /** Compares this run with the baseline environment and writes the triage report; returns null if skipped. */
    public static Path generateReport() {
        if (!isEnabled()) {
            return null;
        }
        Path baseline = snapshotRoot().resolve(baselineEnvironment());
        Path candidate = snapshotRoot().resolve(environment());
        if (environment().equals(baselineEnvironment())) {
            LOGGER.info("VISUAL-TRIAGE: {} is the baseline environment; {} pages saved to {}",
                    environment(), CAPTURED.size(), candidate);
            return null;
        }
        if (!Files.isDirectory(baseline) || !Files.isDirectory(candidate)) {
            LOGGER.warn("VISUAL-TRIAGE: no baseline at {}; run the same tags with -Denv={} -DvisualTriage.enabled=true first",
                    baseline, baselineEnvironment());
            return null;
        }
        try {
            Path out = Paths.get("target/reports/visual-triage-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss")));
            Path report = VisualTriageRunner.run(baseline, candidate, out);
            LOGGER.info("VISUAL-TRIAGE: report {}", report.toAbsolutePath());
            System.out.println("VISUAL-TRIAGE report: " + report.toAbsolutePath());
            return report;
        } catch (Exception e) {
            LOGGER.error("VISUAL-TRIAGE: report generation failed: {}", e.getMessage());
            return null;
        }
    }
}
