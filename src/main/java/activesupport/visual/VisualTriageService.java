package activesupport.visual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.ByteContentFile;
import software.amazon.awssdk.services.bedrockagentruntime.model.FileSource;
import software.amazon.awssdk.services.bedrockagentruntime.model.FileSourceType;
import software.amazon.awssdk.services.bedrockagentruntime.model.FileUseCase;
import software.amazon.awssdk.services.bedrockagentruntime.model.InputFile;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.SessionState;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Asks a Bedrock Agent (e.g. Amazon Nova) to triage a visual difference between a baseline and candidate
 * environment. The prompt is text-first: the pixel-diff region plus a GOV.UK component geometry diff.
 * Cropped PNGs can optionally be attached to the session with {@code -DvisualTriage.attachImages=true}.
 */
public class VisualTriageService implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(VisualTriageService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String INSTRUCTIONS = """
            You are a visual regression reviewer for GOV.UK services built with GOV.UK Frontend.
            BASELINE is the known-good environment, CANDIDATE is the environment under test.
            You receive the changed screen region and a list of component layout differences \
            (x/y are relative to the parent GOV.UK component; pl/pr/ml/mr are padding/margin; align is text-align).
            Report only defects visible in CANDIDATE: misalignment (e.g. a link no longer right-aligned), overlap, \
            clipping, cramped or missing spacing (e.g. an icon touching its text), wrong icon placement, broken wrapping, \
            missing or duplicated components, wrong colours, contrast or focus styles, untranslated text.
            Ignore differences caused only by data values (names, IDs, dates, counts, fees, text length) and changes \
            listed as expected.
            Respond ONLY with JSON:
            {"verdict":"regression|expected|data-only","summary":"...","findings":[{"severity":"high|medium|low",\
            "component":"GOV.UK component or element","description":"what is wrong in CANDIDATE vs BASELINE",\
            "wcag":"criterion if relevant, else empty","confidence":0.0}]}""";

    private final BedrockAgentRuntimeAsyncClient client;
    private final String agentId;
    private final String agentAliasId;

    public VisualTriageService() {
        this(VisualTriageConfig.getAgentId(), VisualTriageConfig.getAgentAliasId());
    }

    public VisualTriageService(String agentId, String agentAliasId) {
        if (agentId == null || agentId.isBlank() || agentAliasId == null || agentAliasId.isBlank()) {
            throw new IllegalStateException("No Bedrock agent configured: set -DvisualTriage.agentId and "
                    + "-DvisualTriage.agentAliasId, or add visualTriage_agentId/visualTriage_agentAliasId "
                    + "to the vol-functional-tests/bedrock secret");
        }
        this.agentId = agentId;
        this.agentAliasId = agentAliasId;
        this.client = BedrockAgentRuntimeAsyncClient.builder()
                .region(Region.of(VisualTriageConfig.getRegion()))
                .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
                .build();
    }

    public Optional<TriageResult> triage(String page, double changedRatio, Rectangle changedArea, List<String> geometryDiffs,
                                         byte[] baselinePng, byte[] candidatePng, byte[] diffPng) {
        try {
            InvokeAgentRequest.Builder request = InvokeAgentRequest.builder()
                    .agentId(agentId)
                    .agentAliasId(agentAliasId)
                    .sessionId(UUID.randomUUID().toString())
                    .inputText(prompt(page, changedRatio, changedArea, geometryDiffs));
            if (VisualTriageConfig.isAttachImages()) {
                request.sessionState(SessionState.builder().files(
                        file("baseline.png", baselinePng),
                        file("candidate.png", candidatePng),
                        file("diff.png", diffPng)).build());
            }

            StringBuilder text = new StringBuilder();
            InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
                    .subscriber(InvokeAgentResponseHandler.Visitor.builder()
                            .onChunk(chunk -> {
                                if (chunk.bytes() != null) {
                                    text.append(chunk.bytes().asUtf8String());
                                }
                            })
                            .build())
                    .onError(error -> LOGGER.warn("VISUAL-TRIAGE: Agent streaming error: {}", error.getMessage()))
                    .build();

            client.invokeAgent(request.build(), handler).get(120, TimeUnit.SECONDS);
            return Optional.of(parse(text.toString()));
        } catch (Exception e) {
            LOGGER.warn("VISUAL-TRIAGE: Agent call failed for {}: {}", page, e.getMessage());
            System.err.println("VISUAL-TRIAGE: Agent call failed for " + page + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    static String prompt(String page, double changedRatio, Rectangle area, List<String> geometryDiffs) {
        StringBuilder sb = new StringBuilder(INSTRUCTIONS)
                .append("\n\nPAGE: ").append(page)
                .append("\nEXPECTED CHANGES (not defects): ").append(VisualTriageConfig.getExpectedChanges())
                .append(String.format("%nPIXELS CHANGED: %.2f%% in region x=%d y=%d w=%d h=%d (page coordinates)",
                        changedRatio * 100, area.x, area.y, area.width, area.height))
                .append("\nCOMPONENT LAYOUT DIFFERENCES (baseline -> candidate):");
        if (geometryDiffs.isEmpty()) {
            sb.append("\n(none captured)");
        }
        geometryDiffs.forEach(d -> sb.append("\n- ").append(d));
        if (VisualTriageConfig.isAttachImages()) {
            sb.append("\nAttached: baseline.png, candidate.png, diff.png (changed pixels in red) cropped to that region.");
        }
        return sb.toString();
    }

    private static InputFile file(String name, byte[] png) {
        return InputFile.builder()
                .name(name)
                .useCase(FileUseCase.CHAT)
                .source(FileSource.builder()
                        .sourceType(FileSourceType.BYTE_CONTENT)
                        .byteContent(ByteContentFile.builder()
                                .mediaType("image/png")
                                .data(SdkBytes.fromByteArray(png))
                                .build())
                        .build())
                .build();
    }

    static TriageResult parse(String text) throws Exception {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        JsonNode json = MAPPER.readTree(start >= 0 && end > start ? text.substring(start, end + 1) : text);
        List<Finding> findings = new ArrayList<>();
        for (JsonNode f : json.path("findings")) {
            findings.add(new Finding(
                    f.path("severity").asText("low"),
                    f.path("component").asText(""),
                    f.path("description").asText(""),
                    f.path("wcag").asText(""),
                    f.path("confidence").asDouble(0)));
        }
        return new TriageResult(json.path("verdict").asText("unknown"), json.path("summary").asText(""), findings);
    }

    @Override
    public void close() {
        client.close();
    }

    public record Finding(String severity, String component, String description, String wcag, double confidence) {
    }

    public record TriageResult(String verdict, String summary, List<Finding> findings) {
    }
}
