package activesupport.visual;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Compares two screenshot folders (same relative file names, e.g. dev/ vs reg/), diffs every pair locally
 * and sends only changed areas to Bedrock. Writes {@code report.html} and {@code results.json}.
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass=activesupport.visual.VisualTriageRunner \
 *   -Dexec.args="baselineDir candidateDir outputDir" -DvisualTriage.enabled=true
 * </pre>
 */
public final class VisualTriageRunner {

    private static final Logger LOGGER = LogManager.getLogger(VisualTriageRunner.class);

    private VisualTriageRunner() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: VisualTriageRunner <baselineDir> <candidateDir> <outputDir>");
            System.exit(2);
        }
        Path out = run(Paths.get(args[0]), Paths.get(args[1]), Paths.get(args[2]));
        System.out.println("Visual triage report: " + out.toAbsolutePath());
    }

    public static Path run(Path baselineDir, Path candidateDir, Path outputDir) throws IOException {
        Files.createDirectories(outputDir.resolve("img"));
        List<PageResult> results = new ArrayList<>();
        double threshold = VisualTriageConfig.getDiffThreshold();
        int maxCalls = VisualTriageConfig.getMaxCalls();
        int calls = 0;

        try (VisualTriageService service = createService()) {
            for (Path rel : pngs(baselineDir)) {
                Path candidateFile = candidateDir.resolve(rel.toString());
                String page = rel.toString().replace('\\', '/');
                if (!Files.exists(candidateFile)) {
                    results.add(PageResult.status(page, "missing-in-candidate"));
                    continue;
                }
                BufferedImage base = ImageIO.read(baselineDir.resolve(rel.toString()).toFile());
                BufferedImage cand = ImageIO.read(candidateFile.toFile());
                ImageDiff diff = ImageDiff.compare(base, cand, VisualTriageConfig.getPixelTolerance());
                List<String> geometry = PageGeometry.compare(
                        sidecar(baselineDir.resolve(rel.toString())), sidecar(candidateFile),
                        VisualTriageConfig.getGeometryTolerance(), VisualTriageConfig.getMaxGeometryDiffs());
                if (diff.getChangedRatio() < threshold && geometry.isEmpty()) {
                    results.add(PageResult.status(page, "unchanged"));
                    continue;
                }

                Rectangle area = diff.paddedArea();
                int maxEdge = VisualTriageConfig.getMaxImageEdge();
                byte[] basePng = ImageDiff.png(ImageDiff.fit(ImageDiff.crop(base, area), maxEdge));
                byte[] candPng = ImageDiff.png(ImageDiff.fit(ImageDiff.crop(cand, area), maxEdge));
                byte[] diffPng = ImageDiff.png(ImageDiff.fit(ImageDiff.crop(diff.getDiffImage(), area), maxEdge));
                String stem = page.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("\\.png$", "");
                Files.write(outputDir.resolve("img/" + stem + "-baseline.png"), basePng);
                Files.write(outputDir.resolve("img/" + stem + "-candidate.png"), candPng);
                Files.write(outputDir.resolve("img/" + stem + "-diff.png"), diffPng);

                PageResult result = new PageResult(page, "changed", diff.getChangedRatio(), stem, geometry, null);
                if (service != null && calls < maxCalls) {
                    calls++;
                    LOGGER.info("VISUAL-TRIAGE: [{}/{}] {}", calls, maxCalls, page);
                    System.out.println("VISUAL-TRIAGE: [" + calls + "/" + maxCalls + "] " + page);
                    result = result.withTriage(service.triage(page, diff.getChangedRatio(), diff.getChangedArea(),
                            geometry, basePng, candPng, diffPng).orElse(null));
                }
                results.add(result);
            }
        }
        for (Path rel : pngs(candidateDir)) {
            if (!Files.exists(baselineDir.resolve(rel.toString()))) {
                results.add(PageResult.status(rel.toString().replace('\\', '/'), "missing-in-baseline"));
            }
        }

        results.sort(Comparator.comparingInt(PageResult::rank).thenComparing(r -> -r.changedRatio()));
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(outputDir.resolve("results.json").toFile(), results);
        Path html = outputDir.resolve("report.html");
        Files.writeString(html, html(results, baselineDir, candidateDir));
        return html;
    }

    private static VisualTriageService createService() {
        if (!VisualTriageConfig.isEnabled()) {
            return null;
        }
        try {
            return new VisualTriageService();
        } catch (Exception e) {
            LOGGER.warn("VISUAL-TRIAGE: agent unavailable, report will contain pixel/layout diffs only: {}", e.getMessage());
            System.out.println("VISUAL-TRIAGE: agent unavailable, pixel/layout diffs only: " + e.getMessage());
            return null;
        }
    }

    private static Path sidecar(Path png) {
        return png.resolveSibling(png.getFileName().toString().replaceAll("(?i)\\.png$", ".json"));
    }

    private static List<Path> pngs(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("Not a directory: " + root);
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> p.toString().toLowerCase().endsWith(".png"))
                    .map(root::relativize).sorted().toList();
        }
    }

    private static String html(List<PageResult> results, Path baselineDir, Path candidateDir) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (PageResult r : results) {
            counts.merge(r.label(), 1L, Long::sum);
        }
        StringBuilder sb = new StringBuilder("""
                <!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><title>Visual triage</title><style>
                body{font-family:Arial,sans-serif;margin:24px;color:#0b0c0c}h2{font-size:19px;margin:32px 0 8px}
                .row{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}.row img{width:100%;border:1px solid #b1b4b6}
                .regression{color:#d4351c}.high{font-weight:bold;color:#d4351c}table{border-collapse:collapse}
                td,th{border:1px solid #b1b4b6;padding:4px 8px;text-align:left}</style></head><body>
                <h1>Visual triage</h1>""");
        sb.append("<p>Baseline: ").append(esc(baselineDir.toString())).append("<br>Candidate: ")
                .append(esc(candidateDir.toString())).append("</p><table><tr><th>Status</th><th>Pages</th></tr>");
        counts.forEach((k, v) -> sb.append("<tr><td>").append(esc(k)).append("</td><td>").append(v).append("</td></tr>"));
        sb.append("</table>");
        for (PageResult r : results) {
            if (r.stem() == null) {
                continue;
            }
            sb.append("<h2 class=\"").append(esc(r.label())).append("\">").append(esc(r.page())).append(" — ")
                    .append(esc(r.label())).append(String.format(" (%.2f%% changed)", r.changedRatio() * 100)).append("</h2>");
            if (r.triage() != null) {
                sb.append("<p>").append(esc(r.triage().summary())).append("</p><ul>");
                for (VisualTriageService.Finding f : r.triage().findings()) {
                    sb.append("<li class=\"").append(esc(f.severity())).append("\">[").append(esc(f.severity()))
                            .append("] ").append(esc(f.component())).append(": ").append(esc(f.description()));
                    if (!f.wcag().isBlank()) {
                        sb.append(" (WCAG ").append(esc(f.wcag())).append(")");
                    }
                    sb.append(String.format(" — confidence %.2f</li>", f.confidence()));
                }
                sb.append("</ul>");
            }
            if (!r.geometry().isEmpty()) {
                sb.append("<details><summary>Layout differences (").append(r.geometry().size())
                        .append(")</summary><ul>");
                r.geometry().forEach(g -> sb.append("<li>").append(esc(g)).append("</li>"));
                sb.append("</ul></details>");
            }
            sb.append("<div class=\"row\">");
            for (String kind : new String[]{"baseline", "candidate", "diff"}) {
                sb.append("<figure><figcaption>").append(kind).append("</figcaption><img alt=\"").append(kind)
                        .append(" ").append(esc(r.page())).append("\" src=\"img/").append(esc(r.stem())).append('-')
                        .append(kind).append(".png\"></figure>");
            }
            sb.append("</div>");
        }
        return sb.append("</body></html>").toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public record PageResult(String page, String status, double changedRatio, String stem, List<String> geometry,
                             VisualTriageService.TriageResult triage) {

        static PageResult status(String page, String status) {
            return new PageResult(page, status, 0, null, List.of(), null);
        }

        PageResult withTriage(VisualTriageService.TriageResult t) {
            return new PageResult(page, status, changedRatio, stem, geometry, t);
        }

        String label() {
            return triage == null ? status : triage.verdict();
        }

        int rank() {
            return switch (label()) {
                case "regression" -> 0;
                case "changed", "unknown" -> 1;
                case "missing-in-candidate", "missing-in-baseline" -> 2;
                case "expected", "data-only" -> 3;
                default -> 4;
            };
        }
    }
}
