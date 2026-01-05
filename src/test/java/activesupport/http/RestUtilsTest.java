package activesupport.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import io.restassured.response.ValidatableResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestUtilsTest {

    private WireMockServer wireMockServer;
    private int port;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        port = wireMockServer.port();

        mockGETcall();
        mockPOSTcall();
        mockPUTcall();
        mockGETwithQueryParam();
    }

    @Test
    public void getCallWithNoParams() {
        String url = "http://localhost:" + port + "/vol";
        ValidatableResponse response = RestUtils.get(url, getHeaders());
        assertEquals(200, response.extract().statusCode());
        assertEquals("code_review", response.extract().body().jsonPath().get("what_time"));
    }

    @Test
    public void post() {
        String url = "http://localhost:" + port + "/vol";
        ValidatableResponse response = RestUtils.post(getJsonBody(), url, getHeaders());
        assertEquals(302, response.extract().statusCode());
        assertEquals("curvy-corner", response.extract().header("Location"));
    }

    @Test
    public void put() {
        String url = "http://localhost:" + port + "/vol";
        ValidatableResponse response = RestUtils.put(getJsonBody(), url, getHeaders());
        assertEquals(200, response.extract().statusCode());
        assertEquals("curvy-corner-part-two", response.extract().header("Location"));
    }

    @Test
    public void getCallWithParams() {
        String url = "http://localhost:" + port + "/vol";
        ValidatableResponse response = RestUtils.getWithQueryParams(url, queryParams(), getHeaders());
        assertEquals(200, response.extract().statusCode());
    }

    @Test
    public void getWithProxy() {
        String url = "http://localhost:" + port + "/vol";
        ValidatableResponse response = RestUtils.getThroughProxy("localhost", port, url, getHeaders());
        assertEquals(200, response.extract().statusCode());
    }

    @NotNull
    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("test", "header");
        return headers;
    }

    private static String getJsonBody() {
        HashMap<String, String> body = new HashMap<>();
        body.put("what_time", "code_review");

        Gson gson = new Gson();
        return gson.toJson(body);
    }

    private Map<String, String> queryParams() {
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("who", "it");
        return queryParam;
    }

    private void mockGETcall() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/vol"))
                .withHeader("test", WireMock.equalTo("header"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getJsonBody())));
    }

    private void mockPOSTcall() {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/vol"))
                .withRequestBody(WireMock.equalTo(getJsonBody()))
                .willReturn(WireMock.aResponse()
                        .withStatus(302)
                        .withHeader("Location", "curvy-corner")));
    }

    private void mockPUTcall() {
        wireMockServer.stubFor(WireMock.put(WireMock.urlEqualTo("/vol"))
                .withRequestBody(WireMock.equalTo(getJsonBody()))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Location", "curvy-corner-part-two")));
    }

    private void mockGETwithQueryParam() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/vol"))
                .withQueryParam("who", WireMock.equalTo("it"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")));
    }

    @AfterEach
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }
}