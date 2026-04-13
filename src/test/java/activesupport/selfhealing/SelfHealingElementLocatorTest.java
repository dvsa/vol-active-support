package activesupport.selfhealing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

class SelfHealingElementLocatorTest {

    @AfterEach
    void cleanup() {
        System.clearProperty("selfHealing.enabled");
    }

    @Test
    void healReturnsEmptyWhenDisabled() {
        System.setProperty("selfHealing.enabled", "false");
        var result = SelfHealingElementLocator.heal(null, "//broken", "XPATH");
        assertTrue(result.isEmpty());
    }

    @Test
    void healReturnsEmptyWhenEnabledButDriverIsNull() {
        System.setProperty("selfHealing.enabled", "true");
        // Passing null driver should be handled gracefully
        var result = SelfHealingElementLocator.heal(null, "//broken", "XPATH");
        assertTrue(result.isEmpty());
    }

    // --- toBy auto-detection tests ---

    @Test
    void toByDetectsXpathFromDoubleSlash() {
        By result = SelfHealingElementLocator.toBy("//input[@id='foo']", "ID");
        assertEquals(By.xpath("//input[@id='foo']"), result);
    }

    @Test
    void toByDetectsXpathFromParenthesisDoubleSlash() {
        By result = SelfHealingElementLocator.toBy("(//div)[1]", "CSS");
        assertEquals(By.xpath("(//div)[1]"), result);
    }

    @Test
    void toByDetectsCssSelectorFromHash() {
        // Agent returns #declarationRead for an ID type — should use cssSelector, not By.id
        By result = SelfHealingElementLocator.toBy("#declarationRead", "ID");
        assertEquals(By.cssSelector("#declarationRead"), result);
    }

    @Test
    void toByDetectsCssSelectorFromDot() {
        By result = SelfHealingElementLocator.toBy(".govuk-button", "XPATH");
        assertEquals(By.cssSelector(".govuk-button"), result);
    }

    @Test
    void toByDetectsCssSelectorFromAttributeBracket() {
        By result = SelfHealingElementLocator.toBy("input[name='email']", "ID");
        assertEquals(By.cssSelector("input[name='email']"), result);
    }

    @Test
    void toByFallsBackToOriginalTypeForPlainId() {
        // Plain ID string with no CSS/XPath markers — should use By.id
        By result = SelfHealingElementLocator.toBy("declarationRead", "ID");
        assertEquals(By.id("declarationRead"), result);
    }

    @Test
    void toByFallsBackToOriginalTypeForPlainName() {
        By result = SelfHealingElementLocator.toBy("email", "NAME");
        assertEquals(By.name("email"), result);
    }

    @Test
    void toByFallsBackToLinkText() {
        By result = SelfHealingElementLocator.toBy("Sign in", "LINKTEXT");
        assertEquals(By.linkText("Sign in"), result);
    }
}
