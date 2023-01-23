package activesupport.browsers;

import activesupport.driver.Browser;

import org.junit.jupiter.api.*;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpResponse.response;


public class BrowserTest {

    private static ClientAndServer mockServer;

    private final String baseURL = "http://localhost:8080";
    private final String resource = "/vol/dummy";

    @BeforeAll
    public static void setMockServer() {
        mockServer = startClientAndServer(8080);
        startMockSite();
    }

    @Test
    public void chromeTest(){
        System.setProperty("browser", "chrome");
        Browser.navigate().get(baseURL.concat(resource));
        assertEquals("Browser Test", Browser.navigate().getTitle());
    }

    @Test
    public void localChromeProxyTest() {
        System.setProperty("browser", "chrome-proxy");
        Browser.setIpAddress("localhost");
        Browser.setPortNumber("8090");
        Browser.navigate().get(baseURL.concat(resource));
        assertEquals("Browser Test", Browser.navigate().getTitle());
    }

    @Test
    public void headlessTest(){
        System.setProperty("browser", "headless");
        Browser.navigate().get(baseURL.concat(resource));
        assertEquals("Browser Test", Browser.navigate().getTitle());
    }

    private static void startMockSite() {
        mockServer
                .when(
                        HttpRequest.request()
                                .withPath("/vol/dummy")
                )
                .respond(
                        response()
                                .withBody(htmlBody())
                );
    }

    private static String htmlBody() {
        return "<html><head><title>Browser Test</title></head><body><h1>Hello from VOL</h1></body></html>";
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (Browser.isBrowserOpen()) {
            Browser.closeBrowser();
        }
        if (mockServer.isRunning()) {
            mockServer.stop();
        }
    }
}