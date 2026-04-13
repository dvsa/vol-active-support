package activesupport.selfhealing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelfHealingConfigTest {

    @Test
    void isDisabledByDefault() {
        System.clearProperty("selfHealing.enabled");
        assertFalse(SelfHealingConfig.isEnabled());
    }

    @Test
    void canBeEnabledViaSystemProperty() {
        System.setProperty("selfHealing.enabled", "true");
        try {
            assertTrue(SelfHealingConfig.isEnabled());
        } finally {
            System.clearProperty("selfHealing.enabled");
        }
    }

    @Test
    void defaultRegionIsEuWest1() {
        System.clearProperty("selfHealing.region");
        assertEquals("eu-west-1", SelfHealingConfig.getRegion());
    }

    @Test
    void agentIdCanBeSetViaSystemProperty() {
        System.setProperty("selfHealing.agentId", "TEST_AGENT");
        try {
            assertEquals("TEST_AGENT", SelfHealingConfig.getAgentId());
        } finally {
            System.clearProperty("selfHealing.agentId");
        }
    }

    @Test
    void agentAliasIdCanBeSetViaSystemProperty() {
        System.setProperty("selfHealing.agentAliasId", "TEST_ALIAS");
        try {
            assertEquals("TEST_ALIAS", SelfHealingConfig.getAgentAliasId());
        } finally {
            System.clearProperty("selfHealing.agentAliasId");
        }
    }

    @Test
    void defaultMaxDomLengthIs50000() {
        System.clearProperty("selfHealing.maxDomLength");
        assertEquals(50000, SelfHealingConfig.getMaxDomLength());
    }

    @Test
    void defaultMinConfidenceIs05() {
        System.clearProperty("selfHealing.minConfidence");
        assertEquals(0.5, SelfHealingConfig.getMinConfidence());
    }

    @Test
    void regionCanBeOverridden() {
        System.setProperty("selfHealing.region", "us-east-1");
        try {
            assertEquals("us-east-1", SelfHealingConfig.getRegion());
        } finally {
            System.clearProperty("selfHealing.region");
        }
    }
}
