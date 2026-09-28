package activesupport.mailPit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailPitTest {
    private static final String RECIPIENT = "operator@example.com";
    private static final String SUBJECT = "A Transport Manager has submitted their details for review";
    private static final String EMAIL_BODY = """
            <p>A transport manager has submitted their details.</p>
            <a href="https://example.com/application/123/transport-managers/details/456/">Review application</a>
            """;

    private WireMockServer server;
    private MailPit mailPit;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        mailPit = new MailPit(null);
        mailPit.setBaseUrl("http://localhost:" + server.port());
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void retrievesTmEmailWithoutRecipientInBody() throws Exception {
        stubMessages(true);
        server.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/message/correct/raw"))
                .willReturn(WireMock.ok(EMAIL_BODY)));

        assertEquals(EMAIL_BODY, mailPit.retrieveTmAppLink(RECIPIENT));
        server.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/v1/message/correct/raw")));
        server.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/v1/message/wrong/raw")));
    }

    @Test
    void doesNotRetrieveRawEmailForAnotherRecipient() {
        stubMessages(false);

        assertThrows(IllegalStateException.class, () -> mailPit.retrieveEmailRawContent(RECIPIENT, SUBJECT));
        server.verify(0, WireMock.getRequestedFor(WireMock.urlPathMatching("/api/v1/message/.*/raw")));
    }

    @Test
    void retriesPasswordResetWhenEmailHasNotArrivedYet() throws Exception {
        String resetLink = "https://example.com/reset/123";
        String message = """
                {"ID":"reset","Subject":"Reset your password","Created":"%s","To":[{"Address":"%s"}]}
                """.formatted(Instant.now(), RECIPIENT);
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/messages"))
                .inScenario("reset")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.okJson("{\"messages\":[]}"))
                .willSetStateTo("available"));
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/messages"))
                .inScenario("reset")
                .whenScenarioStateIs("available")
                .willReturn(WireMock.okJson("{\"messages\":[" + message + "]}")));
        server.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/message/reset/raw"))
                .willReturn(WireMock.ok("<a href=3D\"" + resetLink + "\">Reset password</a>")));

        assertEquals(resetLink, mailPit.retrievePasswordResetLink(RECIPIENT, 0));
        server.verify(2, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/v1/messages")));
    }

    @Test
    void reportsPasswordResetEmailStillMissingAfterRetries() {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/messages"))
                .willReturn(WireMock.okJson("{\"messages\":[]}")));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> mailPit.retrievePasswordResetLink(RECIPIENT, 0));
        assertEquals("Email content not found for the specified email address and subject.",
                exception.getCause().getMessage());
        server.verify(2, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/v1/messages")));
    }

    private void stubMessages(boolean includeCorrectRecipient) {
        String created = Instant.now().toString();
        String wrongMessage = """
                {"ID":"wrong","Subject":"%s","Created":"%s","To":[{"Address":"other@example.com"}]}
                """.formatted(SUBJECT, created);
        String correctMessage = """
                {"ID":"correct","Subject":"%s","Created":"%s","To":[{"Address":"%s"}]}
                """.formatted(SUBJECT, created, RECIPIENT);
        String messages = includeCorrectRecipient ? wrongMessage + "," + correctMessage : wrongMessage;

        server.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/messages"))
                .withQueryParam("q", WireMock.equalTo(RECIPIENT))
                .willReturn(WireMock.okJson("{\"messages\":[" + messages + "]}")));
    }
}
