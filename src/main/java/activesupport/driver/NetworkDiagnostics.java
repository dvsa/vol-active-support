package activesupport.driver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the Chrome performance log and reconstructs what the browser actually did
 * on the network: status codes, redirect chains, net::ERR_* failures, who initiated
 * each request, and whether any URL was requested more than once.
 *
 * Built for navigations that appear to do nothing, where the only WebDriver-level
 * symptom is a downstream wait timeout and a blank screenshot.
 *
 * Requires -DnetworkLogging=true so ChromeSetUp enables the performance log.
 */
public final class NetworkDiagnostics {

    private static final Logger LOGGER = LogManager.getLogger(NetworkDiagnostics.class);

    private NetworkDiagnostics() {
    }

    public static final class NetworkEvent {
        public final String kind;
        public final String url;
        public final Integer status;
        public final String errorText;
        public final String initiator;
        public final String requestId;
        public final String resourceType;
        public final String location;
        public final boolean canceled;
        public final double timestamp;

        NetworkEvent(String kind, String url, Integer status, String errorText, String initiator,
                     String requestId, String resourceType, String location, boolean canceled, double timestamp) {
            this.kind = kind;
            this.url = url;
            this.status = status;
            this.errorText = errorText;
            this.initiator = initiator;
            this.requestId = requestId;
            this.resourceType = resourceType;
            this.location = location;
            this.canceled = canceled;
            this.timestamp = timestamp;
        }

        public boolean isFailure() {
            return errorText != null || (status != null && status >= 400);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-9s", kind));
            if (status != null) {
                sb.append(" status=").append(status);
            }
            if (errorText != null) {
                sb.append(" error=").append(errorText);
            }
            if (canceled) {
                sb.append(" CANCELED");
            }
            if (initiator != null && !initiator.isEmpty()) {
                sb.append(" initiator=").append(initiator);
            }
            if (resourceType != null && !resourceType.isEmpty()) {
                sb.append(" type=").append(resourceType);
            }
            if (requestId != null && !requestId.isEmpty()) {
                sb.append(" reqId=").append(requestId);
            }
            sb.append(' ').append(url);
            if (location != null && !location.isEmpty()) {
                sb.append("  -> Location: ").append(location);
            }
            return sb.toString();
        }
    }

    /**
     * Drains the performance log. The driver clears the log on read, so call this
     * once per navigation of interest.
     */
    public static List<NetworkEvent> drain(WebDriver driver) {
        List<NetworkEvent> events = new ArrayList<>();
        if (driver == null) {
            LOGGER.warn("No driver supplied; cannot read performance logs");
            return events;
        }
        try {
            for (LogEntry entry : driver.manage().logs().get(LogType.PERFORMANCE)) {
                NetworkEvent event = parse(entry.getMessage());
                if (event != null) {
                    events.add(event);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Performance logs unavailable for this session ({}). "
                    + "Was the run started with -DnetworkLogging=true?", e.getMessage());
        }
        return events;
    }

    private static NetworkEvent parse(String raw) {
        JsonObject message;
        try {
            JsonElement root = JsonParser.parseString(raw);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("message")) {
                return null;
            }
            message = root.getAsJsonObject().getAsJsonObject("message");
        } catch (Exception e) {
            return null;
        }

        if (message == null || !message.has("method")) {
            return null;
        }

        String method = message.get("method").getAsString();
        JsonObject params = message.has("params") && message.get("params").isJsonObject()
                ? message.getAsJsonObject("params")
                : new JsonObject();

        String requestId = string(params, "requestId");
        double timestamp = params.has("timestamp") ? params.get("timestamp").getAsDouble() : 0d;
        String resourceType = string(params, "type");

        switch (method) {
            case "Network.requestWillBeSent": {
                String url = nested(params, "request", "url");
                String initiator = nested(params, "initiator", "type");

                // A redirectResponse means this event is the *next* hop of a chain; the
                // status belongs to the hop that sent us here.
                if (params.has("redirectResponse") && params.get("redirectResponse").isJsonObject()) {
                    JsonObject redirect = params.getAsJsonObject("redirectResponse");
                    Integer status = redirect.has("status") ? redirect.get("status").getAsInt() : null;
                    String from = redirect.has("url") ? redirect.get("url").getAsString() : "";
                    String location = header(redirect, "location");
                    return new NetworkEvent("REDIRECT", from + "  ==>  " + url, status, null,
                            initiator, requestId, resourceType, location, false, timestamp);
                }
                return new NetworkEvent("REQUEST", url, null, null, initiator, requestId,
                        resourceType, null, false, timestamp);
            }
            case "Network.responseReceived": {
                String url = nested(params, "response", "url");
                Integer status = null;
                String location = null;
                if (params.has("response") && params.get("response").isJsonObject()) {
                    JsonObject response = params.getAsJsonObject("response");
                    if (response.has("status")) {
                        status = response.get("status").getAsInt();
                    }
                    location = header(response, "location");
                }
                return new NetworkEvent("RESPONSE", url, status, null, null, requestId,
                        resourceType, location, false, timestamp);
            }
            case "Network.loadingFailed": {
                String errorText = string(params, "errorText");
                boolean canceled = params.has("canceled") && params.get("canceled").getAsBoolean();
                return new NetworkEvent("FAILED", "(requestId " + requestId + ")", null,
                        errorText.isEmpty() ? "unknown" : errorText, null, requestId,
                        resourceType, null, canceled, timestamp);
            }
            default:
                return null;
        }
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private static String nested(JsonObject params, String parent, String child) {
        if (params.has(parent) && params.get(parent).isJsonObject()) {
            return string(params.getAsJsonObject(parent), child);
        }
        return "";
    }

    private static String header(JsonObject response, String name) {
        if (response.has("headers") && response.get("headers").isJsonObject()) {
            JsonObject headers = response.getAsJsonObject("headers");
            for (String key : headers.keySet()) {
                if (key.equalsIgnoreCase(name)) {
                    return string(headers, key);
                }
            }
        }
        return "";
    }

    /**
     * Logs the whole trace, then repeats failures and any repeated request so they
     * are findable in a large CI log.
     */
    public static void report(WebDriver driver, String context) {
        log(drain(driver), context, null);
    }

    /**
     * Logs only events whose URL contains the given fragment, e.g. "account.gov.uk".
     * Duplicate detection still runs across the filtered set.
     */
    public static void reportMatching(WebDriver driver, String context, String urlFragment) {
        List<NetworkEvent> all = drain(driver);
        List<NetworkEvent> filtered = all.stream()
                .filter(event -> event.url != null && event.url.contains(urlFragment))
                .toList();
        log(filtered, context, urlFragment);
    }

    private static void log(List<NetworkEvent> events, String context, String urlFragment) {
        String scope = urlFragment == null ? context : context + "' matching '" + urlFragment;

        if (events.isEmpty()) {
            LOGGER.info("=== NETWORK TRACE [{}] - no events captured ===", scope);
            return;
        }

        LOGGER.info("=== NETWORK TRACE [{}] - {} events ===", scope, events.size());
        double start = events.get(0).timestamp;
        for (NetworkEvent event : events) {
            LOGGER.info("  +{}s  {}", String.format("%.3f", event.timestamp - start), event);
        }

        List<NetworkEvent> failures = events.stream().filter(NetworkEvent::isFailure).toList();
        if (!failures.isEmpty()) {
            LOGGER.info("=== NETWORK FAILURES [{}] ===", scope);
            failures.forEach(failure -> LOGGER.info("  {}", failure));
        }

        logRepeats(events, scope);
    }

    /**
     * Flags any URL requested more than once. A single-use OAuth authorisation code
     * being requested twice is the signature of the callback replay, and the
     * initiator on the second request says whether the browser or a script sent it.
     */
    private static void logRepeats(List<NetworkEvent> events, String scope) {
        Map<String, List<NetworkEvent>> byUrl = new LinkedHashMap<>();
        for (NetworkEvent event : events) {
            if (!"REQUEST".equals(event.kind) || event.url == null || event.url.isEmpty()) {
                continue;
            }
            byUrl.computeIfAbsent(event.url, key -> new ArrayList<>()).add(event);
        }

        boolean headerLogged = false;
        for (Map.Entry<String, List<NetworkEvent>> entry : byUrl.entrySet()) {
            List<NetworkEvent> repeats = entry.getValue();
            if (repeats.size() < 2) {
                continue;
            }
            if (!headerLogged) {
                LOGGER.info("=== REPEATED REQUESTS [{}] - same URL sent more than once ===", scope);
                headerLogged = true;
            }
            LOGGER.info("  {} times: {}", repeats.size(), entry.getKey());
            double first = repeats.get(0).timestamp;
            for (NetworkEvent repeat : repeats) {
                LOGGER.info("      +{}s  initiator={}  reqId={}",
                        String.format("%.3f", repeat.timestamp - first),
                        repeat.initiator == null || repeat.initiator.isEmpty() ? "unknown" : repeat.initiator,
                        repeat.requestId);
            }
        }
    }

    /**
     * Logs the browser console log. Useful alongside the network trace, since a
     * blank page with no network failure usually has a console error behind it.
     */
    public static void reportBrowserLog(WebDriver driver, String context) {
        if (driver == null) {
            return;
        }
        try {
            List<LogEntry> entries = driver.manage().logs().get(LogType.BROWSER).getAll();
            if (entries.isEmpty()) {
                LOGGER.info("=== BROWSER LOG [{}] - empty ===", context);
                return;
            }
            LOGGER.info("=== BROWSER LOG [{}] - {} entries ===", context, entries.size());
            entries.forEach(entry -> LOGGER.info("  [{}] {}", entry.getLevel(), entry.getMessage()));
        } catch (Exception e) {
            LOGGER.warn("Browser logs unavailable: {}", e.getMessage());
        }
    }

    /**
     * Everything in one call: current URL, page title, network trace and console log.
     * Safe to call from a catch block - never throws.
     */
    public static void reportAll(WebDriver driver, String context) {
        try {
            if (driver != null) {
                LOGGER.info("=== PAGE STATE [{}] ===", context);
                LOGGER.info("  currentUrl: {}", driver.getCurrentUrl());
                LOGGER.info("  title     : {}", driver.getTitle());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read page state: {}", e.getMessage());
        }
        try {
            reportBrowserLog(driver, context);
            report(driver, context);
        } catch (Exception e) {
            LOGGER.warn("Network diagnostics failed: {}", e.getMessage());
        }
    }
}
