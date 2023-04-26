package activesupport.mailhog;

import activesupport.http.RestUtils;
import io.restassured.response.ValidatableResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mailhog {
    ValidatableResponse response;
    private String ip;
    private String port;

    public String retrievePassword(String emailAddress) {
        if (getIp() == null) {
            setIp("http://localhost");
            setPort("8025");
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("test", "dvsa");

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("kind", "containing");
        queryParams.put("query", emailAddress.concat(" : Your temporary password"));

        response = RestUtils.getWithQueryParams(String.format("%s:%s/api/v2/search/", getIp(), getPort()), queryParams, headers);
        return extractTempPassword(response.extract().jsonPath().prettyPeek().getString("items.Content.Body"));
    }

    private static String extractTempPassword(String apiResponseBody) {
        String mailServerContents = new Scanner(apiResponseBody).useDelimiter("\\A").next();
        Pattern pattern = Pattern.compile("[.\\w\\S]{0,30}(?==0ASign)");
        Matcher matcher = pattern.matcher(mailServerContents);
        matcher.find();
        return matcher.group();
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}