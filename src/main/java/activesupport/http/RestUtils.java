package activesupport.http;

import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class RestUtils {

    private static ValidatableResponse response;

    public static ValidatableResponse post(@NotNull Object requestBody, @NotNull String serviceEndPoint, @NotNull Map<String, String> headers) {
        response = given()
                .urlEncodingEnabled(true)
                .log().all()
                .contentType(ContentType.JSON)
                .headers(headers)
                .body(requestBody)
                .when().config(RestAssuredConfig.config().sslConfig(new SSLConfig().relaxedHTTPSValidation().allowAllHostnames()))
                .post(serviceEndPoint)
                .then();
        return response;
    }

    public static ValidatableResponse put(@NotNull Object requestBody, @NotNull String serviceEndPoint, @NotNull Map<String, String> headers) {
        response = given()
                .urlEncodingEnabled(true)
                .log().all()
                .contentType(ContentType.JSON)
                .headers(headers)
                .body(requestBody)
                .when().config(RestAssuredConfig.config().sslConfig(new SSLConfig().relaxedHTTPSValidation().allowAllHostnames()))
                .put(serviceEndPoint)
                .then();
        return response;
    }

    public static ValidatableResponse getWithQueryParams(@NotNull String serviceEndPoint, @Nullable Map<String,String> queryParam, @NotNull Map<String, String> headers) {
        response = given().params(queryParam)
                .urlEncodingEnabled(true)
                .log().all()
                .headers(headers)
                .when().config(RestAssuredConfig.config().sslConfig(new SSLConfig().relaxedHTTPSValidation().allowAllHostnames()))
                .get(serviceEndPoint)
                .then();
        return response;
    }

    public static ValidatableResponse get(@NotNull String serviceEndPoint, @NotNull Map<String, String> headers) {
        response = given()
                .urlEncodingEnabled(true)
                .log().all()
                .headers(headers)
                .when().config(RestAssuredConfig.config().sslConfig(new SSLConfig().relaxedHTTPSValidation().allowAllHostnames()))
                .get(serviceEndPoint)
                .then();
        return response;
    }

    public static ValidatableResponse delete(@NotNull Object requestBody, @NotNull String serviceEndPoint, @NotNull Map<String, String> headers) {
        response = given()
                .urlEncodingEnabled(true)
                .log().all()
                .contentType(ContentType.JSON)
                .headers(headers)
                .body(requestBody)
                .when().config(RestAssuredConfig.config().sslConfig(new SSLConfig().relaxedHTTPSValidation().allowAllHostnames()))
                .delete(serviceEndPoint)
                .then();
        return response;
    }
}