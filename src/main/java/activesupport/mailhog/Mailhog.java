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

    public String retrievePassword(String emailSubject) {
        if (getIp().equals("")) {
            setIp("http://localhost");
            setPort("8025");
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");

        RestUtils.get(String.format("%s:%s", getIp(), getPort()), headers);
        return extractTempPassword(response.extract().jsonPath().getString("items.Content.Headers.Body"));
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
