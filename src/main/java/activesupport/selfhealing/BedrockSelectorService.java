package activesupport.selfhealing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BedrockSelectorService {

    private static final Logger LOGGER = LogManager.getLogger(BedrockSelectorService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BedrockAgentRuntimeAsyncClient client;

    private BedrockAgentRuntimeAsyncClient getClient() {
        if (client == null) {
            client = BedrockAgentRuntimeAsyncClient.builder()
                    .region(Region.of(SelfHealingConfig.getRegion()))
                    .build();
        }
        return client;
    }

    public Optional<HealResult> findCorrectedSelector(String brokenSelector, String selectorType, String pageSource) {
        try {
            String truncatedDom = truncateDom(pageSource, SelfHealingConfig.getMaxDomLength());
            String prompt = buildPrompt(brokenSelector, selectorType, truncatedDom);

            LOGGER.info("SELF-HEALING: Querying Bedrock agent for broken {} selector: {}", selectorType, brokenSelector);

            String sessionId = UUID.randomUUID().toString();
            StringBuilder responseText = new StringBuilder();

            InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
                    .onResponse(response -> LOGGER.debug("SELF-HEALING: Agent response received"))
                    .subscriber(InvokeAgentResponseHandler.Visitor.builder()
                            .onChunk(chunk -> {
                                if (chunk.bytes() != null) {
                                    responseText.append(chunk.bytes().asUtf8String());
                                }
                            })
                            .build())
                    .onError(error -> LOGGER.error("SELF-HEALING: Agent streaming error", error))
                    .build();

            CompletableFuture<Void> future = getClient().invokeAgent(
                    InvokeAgentRequest.builder()
                            .agentId(SelfHealingConfig.getAgentId())
                            .agentAliasId(SelfHealingConfig.getAgentAliasId())
                            .sessionId(sessionId)
                            .inputText(prompt)
                            .build(),
                    handler
            );

            future.get(30, TimeUnit.SECONDS);

            return parseResponse(responseText.toString());

        } catch (Exception e) {
            LOGGER.warn("SELF-HEALING: Failed to query Bedrock agent: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(String brokenSelector, String selectorType, String dom) {
        return String.format(
                "You are a Selenium selector repair assistant. A %s selector is broken and needs fixing.\n\n"
                + "BROKEN SELECTOR: %s\n\n"
                + "RULES:\n"
                + "1. Analyse the HTML below and find the correct selector for the same target element.\n"
                + "2. PRESERVE positional indices like [2], [last()], [position()>1] etc. — they exist because "
                + "multiple elements match and the test targets a specific one (e.g. a visible checkbox vs a hidden input).\n"
                + "3. The corrected selector MUST target a VISIBLE, INTERACTABLE element — never a type=\"hidden\" input "
                + "when the original clearly intended a clickable/visible control.\n"
                + "4. Only fix what is broken (e.g. a typo in an attribute value). Do not restructure or simplify the selector.\n"
                + "5. Respond ONLY with JSON: {\"selector\": \"...\", \"confidence\": 0.0-1.0, \"reason\": \"...\"}\n\n"
                + "PAGE HTML:\n%s",
                selectorType, brokenSelector, dom
        );
    }

    private Optional<HealResult> parseResponse(String response) {
        try {
            String jsonStr = extractJson(response);
            JsonNode json = MAPPER.readTree(jsonStr);

            String selector = json.get("selector").asText();
            double confidence = json.get("confidence").asDouble();
            String reason = json.has("reason") ? json.get("reason").asText() : "unknown";

            if (selector == null || selector.isBlank()) {
                LOGGER.warn("SELF-HEALING: Agent returned empty selector");
                return Optional.empty();
            }

            LOGGER.info("SELF-HEALING: Agent suggested selector: {} (confidence: {}, reason: {})",
                    selector, confidence, reason);

            return Optional.of(new HealResult(selector, confidence, reason));

        } catch (Exception e) {
            LOGGER.warn("SELF-HEALING: Failed to parse agent response: {} | Raw response: {}",
                    e.getMessage(), response);
            return Optional.empty();
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    static String truncateDom(String pageSource, int maxLength) {
        if (pageSource == null) return "";
        // Strip script and style content to maximise useful DOM in the context window
        String cleaned = pageSource
                .replaceAll("(?s)<script[^>]*>.*?</script>", "")
                .replaceAll("(?s)<style[^>]*>.*?</style>", "")
                .replaceAll("\\s{2,}", " ");

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength) + "\n<!-- DOM TRUNCATED -->";
    }
}
