package activesupport.selfhealing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BedrockSelectorServiceTest {

    @Test
    void truncateDomStripsScriptTags() {
        String html = "<html><body><script>var x = 1;</script><div id='main'>Hello</div></body></html>";
        String result = BedrockSelectorService.truncateDom(html, 50000);
        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("<div id='main'>Hello</div>"));
    }

    @Test
    void truncateDomStripsStyleTags() {
        String html = "<html><head><style>.x { color: red; }</style></head><body><p>Content</p></body></html>";
        String result = BedrockSelectorService.truncateDom(html, 50000);
        assertFalse(result.contains("<style>"));
        assertTrue(result.contains("<p>Content</p>"));
    }

    @Test
    void truncateDomRespectsMaxLength() {
        String html = "<div>" + "a".repeat(100000) + "</div>";
        String result = BedrockSelectorService.truncateDom(html, 1000);
        assertTrue(result.length() <= 1000 + "<!-- DOM TRUNCATED -->".length() + 1);
        assertTrue(result.endsWith("<!-- DOM TRUNCATED -->"));
    }

    @Test
    void truncateDomHandlesNull() {
        assertEquals("", BedrockSelectorService.truncateDom(null, 50000));
    }

    @Test
    void truncateDomCollapsesWhitespace() {
        String html = "<div>   \n\n   <p>text</p>   \n   </div>";
        String result = BedrockSelectorService.truncateDom(html, 50000);
        assertFalse(result.contains("   "));
    }

    @Test
    void truncateDomReturnFullDomWhenUnderLimit() {
        String html = "<div><p>Short content</p></div>";
        String result = BedrockSelectorService.truncateDom(html, 50000);
        assertFalse(result.contains("TRUNCATED"));
    }
}
