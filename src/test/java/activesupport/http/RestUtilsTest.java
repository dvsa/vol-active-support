package activesupport.http;

import com.google.gson.Gson;
import io.restassured.response.ValidatableResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpResponse.response;

public class RestUtilsTest {


    private ClientAndServer mockServer;


    public RestUtilsTest() {
        try {
            mockServer = startClientAndServer(1080);
            mockGETcall();
            mockPOSTcall();
            mockPUTcall();
            mockGETwithQueryParam();
        }catch (Exception e){
            e.getMessage();
        }
    }


    @Test
    public void getCallWithNoParams() {
        String url = "/vol";
        ValidatableResponse response = RestUtils.get(url, getHeaders());
        assertEquals(200, response.extract().statusCode());
        assertEquals(response.extract().body().jsonPath().get("what_time"), "code_review");
    }

    @Test
    public void post() {
        String url = "/vol";
        ValidatableResponse response = RestUtils.post(getJsonBody(),url, getHeaders());
        assertEquals(302, response.extract().statusCode());
        assertEquals(response.extract().header("Location"), "curvy-corner");
    }

    @Test
    public void put() {
        mockPUTcall();
        String url = "/vol";
        ValidatableResponse response = RestUtils.put(getJsonBody(),url, getHeaders());
        assertEquals(200, response.extract().statusCode());
        assertEquals(response.extract().header("Location"), "curvy-corner-part-two");
    }

    @Test
    public void getCallWithParams() {
        mockGETwithQueryParam();
        String url = "/vol";
        ValidatableResponse response = RestUtils.getWithQueryParams(url, queryParams(), getHeaders());
        assertEquals(200, response.extract().statusCode());
    }

    @Test
    public void getWithProxy() {
        mockGETcall();
        String url = "/vol";
        ValidatableResponse response = RestUtils.getThroughProxy("localhost", 8080, url, getHeaders());
        assertEquals(200, response.extract().statusCode());

    }

    @NotNull
    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        {
            headers.put("test", "header");
        }
        return headers;
    }

    private static String getJsonBody() {
        HashMap<String, String> body = new HashMap<>();
        {
            body.put("what_time", "code_review");
        }

        Gson gson = new Gson();
        return gson.toJson(body);
    }


    private Map<String, String> queryParams() {
        Map<String, String> queryParam = new HashMap<>();
        {
            queryParam.put("who", "it");
        }
        return queryParam;
    }

    public void mockGETcall() {
        mockServer
                .when(
                        HttpRequest.request()
                                .withMethod("GET")
                                .withHeader("test", "header")
                                .withPath("/vol")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(getJsonBody())
                );
    }

    private void mockPOSTcall() {
        mockServer
                .when(
                        HttpRequest.request()
                                .withMethod("POST")
                                .withPath("/vol")
                                .withBody(getJsonBody())
                )
                .respond(
                        response()
                                .withStatusCode(302)
                                .withHeader("Location", "curvy-corner")
                );
    }

    private void mockPUTcall() {
        mockServer
                .when(
                        HttpRequest.request()
                                .withMethod("PUT")
                                .withPath("/vol")
                                .withBody(getJsonBody())
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader("Location", "curvy-corner-part-two")
                );
    }

    private void mockGETwithQueryParam() {
        mockServer
                .when(
                        HttpRequest.request()
                                .withMethod("GET")
                                .withQueryStringParameter(new Parameter("who", "it"))
                                .withPath("/vol")

                )
                .respond(
                        response()
                                .withStatusCode(200)
                );
    }


    @AfterEach
    public void tearDown() {
        if(mockServer.isRunning())
        mockServer.stop();
    }
}