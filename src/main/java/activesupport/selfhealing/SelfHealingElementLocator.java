package activesupport.selfhealing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

public class SelfHealingElementLocator {

    private static final Logger LOGGER = LogManager.getLogger(SelfHealingElementLocator.class);
    private static final BedrockSelectorService bedrockService = new BedrockSelectorService();

    private SelfHealingElementLocator() {
    }

    /**
     * Attempts to heal a broken selector by analysing the current page DOM with AI.
     * Returns the healed WebElement if successful, or empty if healing is disabled,
     * fails, or the confidence is too low.
     *
     * @param driver          the active WebDriver instance
     * @param brokenSelector  the selector string that failed
     * @param selectorType    the type of selector (CSS, XPATH, ID, etc.)
     * @return Optional containing the found WebElement if healing succeeded
     */
    public static Optional<WebElement> heal(WebDriver driver, String brokenSelector, String selectorType) {
        if (!SelfHealingConfig.isEnabled()) {
            return Optional.empty();
        }

        try {
            LOGGER.warn("SELF-HEALING: Attempting to heal broken {} selector: {}", selectorType, brokenSelector);

            String pageSource = driver.getPageSource();
            Optional<HealResult> result = bedrockService.findCorrectedSelector(brokenSelector, selectorType, pageSource);

            if (result.isEmpty()) {
                LOGGER.warn("SELF-HEALING: Agent could not find a replacement selector");
                return Optional.empty();
            }

            HealResult heal = result.get();

            if (heal.getConfidence() < SelfHealingConfig.getMinConfidence()) {
                LOGGER.warn("SELF-HEALING: Confidence too low ({}) for selector: {}",
                        heal.getConfidence(), heal.getSelector());
                return Optional.empty();
            }

            By healedBy = toBy(heal.getSelector(), selectorType);
            WebElement element = driver.findElement(healedBy);

            // If the healed element is a hidden input, look for a visible sibling with the same name
            if (!element.isDisplayed()) {
                LOGGER.warn("SELF-HEALING: Healed element is not displayed, searching for visible alternative");
                java.util.List<WebElement> candidates = driver.findElements(healedBy);
                Optional<WebElement> visible = candidates.stream()
                        .filter(WebElement::isDisplayed)
                        .findFirst();
                if (visible.isPresent()) {
                    element = visible.get();
                    LOGGER.info("SELF-HEALING: Found visible alternative (element {} of {})",
                            candidates.indexOf(element) + 1, candidates.size());
                } else {
                    LOGGER.warn("SELF-HEALING: No visible alternative found — using original healed element");
                }
            }

            LOGGER.warn(
                    "\n╔══════════════════════════════════════════════════════════════╗\n" +
                    "║  SELF-HEALED SELECTOR                                        ║\n" +
                    "╠══════════════════════════════════════════════════════════════╣\n" +
                    "║  Type:       {}\n" +
                    "║  Broken:     {}\n" +
                    "║  Healed:     {}\n" +
                    "║  Confidence: {}\n" +
                    "║  Reason:     {}\n" +
                    "║  Page:       {}\n" +
                    "╠══════════════════════════════════════════════════════════════╣\n" +
                    "║  ⚠  UPDATE YOUR TEST CODE WITH THE HEALED SELECTOR          ║\n" +
                    "╚══════════════════════════════════════════════════════════════╝",
                    selectorType, brokenSelector, heal.getSelector(),
                    heal.getConfidence(), heal.getReason(), driver.getCurrentUrl()
            );

            return Optional.of(element);

        } catch (Exception e) {
            LOGGER.warn("SELF-HEALING: Healing attempt failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Converts the healed selector string to a Selenium By locator.
     * Auto-detects the format returned by the AI agent rather than
     * blindly trusting the original selectorType — the agent may return
     * a CSS selector (#id) even when the broken selector was type ID.
     */
    static By toBy(String selector, String selectorType) {
        // Agent returned an XPath expression
        if (selector.startsWith("//") || selector.startsWith("(//")) {
            return By.xpath(selector);
        }

        // Agent returned a CSS #id selector — use cssSelector, not By.id
        if (selector.startsWith("#") || selector.startsWith(".") || selector.contains("[")) {
            return By.cssSelector(selector);
        }

        // No prefix detected — fall back to the original type
        return switch (selectorType.toUpperCase()) {
            case "CSS" -> By.cssSelector(selector);
            case "XPATH" -> By.xpath(selector);
            case "ID" -> By.id(selector);
            case "NAME" -> By.name(selector);
            case "LINKTEXT" -> By.linkText(selector);
            case "PARTIALLINKTEXT" -> By.partialLinkText(selector);
            default -> By.cssSelector(selector);
        };
    }
}
