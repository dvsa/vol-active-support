package activesupport.browsers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BrowserTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    public void setMockServer() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><body>Test Page</body></html>")));
    }

    @Test
    public void testBrowser() {
        assertTrue(wireMockServer.isRunning());
        System.out.println("WireMock server running on port: " + wireMockServer.port());
    }

    @AfterEach
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }
}