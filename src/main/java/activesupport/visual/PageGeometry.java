package activesupport.visual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures the layout of GOV.UK components (box relative to the parent component, spacing, alignment)
 * so layout regressions can be described to a text-only agent. Saved as a {@code .json} sidecar
 * next to each screenshot.
 */
public final class PageGeometry {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CAPTURE_JS = """
            const roots = '.govuk-header,.govuk-service-navigation,.govuk-phase-banner,.govuk-notification-banner,\
            .govuk-warning-text,.govuk-error-summary,.govuk-panel,.govuk-summary-list,.govuk-table,.govuk-tabs,\
            .govuk-form-group,.govuk-fieldset,.govuk-inset-text,.govuk-details,.govuk-footer,main,form';
            const targets = roots + ',[class*="govuk-notification-banner__"],[class*="govuk-warning-text__"],\
            [class*="govuk-header__"],[class*="govuk-footer__"],[class*="govuk-tabs__"],[class*="govuk-summary-list__"],\
            [class*="govuk-table__"],[class*="govuk-error-summary__"],.govuk-button,.govuk-tag,.govuk-label,.govuk-hint,\
            .govuk-input,.govuk-select,.govuk-textarea,.govuk-radios__item,.govuk-checkboxes__item,.govuk-back-link,\
            .govuk-breadcrumbs,h1,h2,h3,[lang] > a,[aria-current]';
            const seen = {}; const out = {};
            for (const el of document.querySelectorAll(targets)) {
              const r = el.getBoundingClientRect();
              if (r.width === 0 && r.height === 0) continue;
              const cls = [...el.classList].filter(c => c.startsWith('govuk-')).sort().join('.');
              const base = el.tagName.toLowerCase() + (cls ? '.' + cls : '');
              const n = seen[base] = (seen[base] || 0) + 1;
              const parent = el.parentElement ? el.parentElement.closest(roots) : null;
              const p = parent ? parent.getBoundingClientRect() : {left: 0, top: 0};
              const s = getComputedStyle(el);
              out[base + '#' + n] = {
                x: Math.round(r.left - p.left), y: Math.round(r.top - p.top),
                w: Math.round(r.width), h: Math.round(r.height),
                pl: s.paddingLeft, pr: s.paddingRight, ml: s.marginLeft, mr: s.marginRight,
                align: s.textAlign, display: s.display, float: s.float, position: s.position,
                font: s.fontSize + ' ' + s.fontWeight, color: s.color, bg: s.backgroundColor,
                text: (el.innerText || '').trim().replace(/\\s+/g, ' ').slice(0, 40)
              };
              if (Object.keys(out).length >= 600) break;
            }
            return JSON.stringify({width: window.innerWidth, components: out});""";

    private PageGeometry() {
    }

    public static String capture(WebDriver driver) {
        return (String) ((JavascriptExecutor) driver).executeScript(CAPTURE_JS);
    }

    public static void captureTo(WebDriver driver, Path jsonFile) throws IOException {
        Files.createDirectories(jsonFile.getParent());
        Files.writeString(jsonFile, capture(driver));
    }

    /**
     * Returns human-readable layout differences, e.g.
     * {@code a.govuk-notification-banner__link#1 ("Close"): x 1180 -> 610}.
     */
    public static List<String> compare(Path baselineJson, Path candidateJson, int tolerance, int max) throws IOException {
        if (!Files.exists(baselineJson) || !Files.exists(candidateJson)) {
            return List.of();
        }
        JsonNode base = MAPPER.readTree(baselineJson.toFile()).path("components");
        JsonNode cand = MAPPER.readTree(candidateJson.toFile()).path("components");
        List<String> diffs = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = base.fields(); it.hasNext() && diffs.size() < max; ) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode b = e.getValue();
            JsonNode c = cand.get(e.getKey());
            String label = e.getKey() + label(b);
            if (c == null) {
                diffs.add(label + ": missing in candidate");
                continue;
            }
            Map<String, String> changes = new LinkedHashMap<>();
            for (String k : new String[]{"x", "y", "w", "h"}) {
                if (Math.abs(b.path(k).asInt() - c.path(k).asInt()) > tolerance) {
                    changes.put(k, b.path(k).asInt() + " -> " + c.path(k).asInt());
                }
            }
            for (String k : new String[]{"pl", "pr", "ml", "mr", "align", "display", "float", "position", "font", "color", "bg"}) {
                if (!b.path(k).asText().equals(c.path(k).asText())) {
                    changes.put(k, b.path(k).asText() + " -> " + c.path(k).asText());
                }
            }
            if (!changes.isEmpty()) {
                diffs.add(label + ": " + changes.toString().replaceAll("[{}]", "").replace("=", " "));
            }
        }
        for (Iterator<String> it = cand.fieldNames(); it.hasNext() && diffs.size() < max; ) {
            String key = it.next();
            if (!base.has(key)) {
                diffs.add(key + label(cand.get(key)) + ": new in candidate");
            }
        }
        return diffs;
    }

    private static String label(JsonNode n) {
        String text = n.path("text").asText("");
        return text.isBlank() ? "" : " (\"" + text + "\")";
    }
}
