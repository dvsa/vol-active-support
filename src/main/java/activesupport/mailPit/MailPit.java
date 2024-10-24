package activesupport.mailPit;

import activesupport.MissingRequiredArgument;
import activesupport.http.RestUtils;
import io.restassured.response.ValidatableResponse;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailPit {
    ValidatableResponse response;
    private String ip;
    private String port;

    public MailPit() {
        this.ip = "https://selenium-mail.olcs.dev-dvsacloud.uk/";
        this.port = "8025";
    }


    public String retrieveTempPassword(String emailAddress) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("kind", "containing");
        queryParams.put("query", emailAddress.concat(" : Your temporary password"));


        response = RestUtils.getWithQueryParams(String.format("%s:%s/api/v2/search/", getIp(), getPort()), queryParams, getHeaders());
        return extractTempPassword(response.extract().jsonPath().getString("items.Content.Body"));
    }


    public String retrieveEmailContent(String emailAddress, String subjectContains) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("kind", "containing");
        queryParams.put("query", emailAddress.concat(" : ").concat(subjectContains));

        response = RestUtils.getWithQueryParams(String.format("%s:%s/api/v2/search/", getIp(), getPort()), queryParams, getHeaders());
        return response.extract().jsonPath().getString("items.Content.Body");
    }


    public String retrieveSignInCode(String emailAddress) {
        String emailContent = retrieveEmailContent(emailAddress, "Your sign-in code");
        return extractSignInCode(emailContent);
    }


    public String retrieveTmAppLink(String emailAddress) throws MissingRequiredArgument {
        String emailContent = retrieveEmailContent(emailAddress, "A Transport Manager has submitted their details for review");
        return extractLinkFromEmail(emailContent);
    }


    public String retrievePasswordResetLink(@NotNull String emailAddress) throws MissingRequiredArgument {
        try {
            TimeUnit.SECONDS.sleep(10);
            String emailContent = retrieveEmailContent(emailAddress, "Reset your password");
            return extractLinkFromEmail(emailContent);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public String retrieveUsernameInfo(String emailAddress) throws MissingRequiredArgument {
        String emailContent = retrieveEmailContent(emailAddress, "Your account information");
        return extractUsernameFromEmail(emailContent);
    }


    public boolean checkLastTMLetterAttachment(@NotNull String emailAddress, String licenceNo) throws MissingRequiredArgument {
        String emailContent = retrieveEmailContent(emailAddress, "Urgent Removal of last Transport Manager");
        return emailContent.contains(String.format("%s_Last_TM_letter_Licence_%s", licenceNo, licenceNo));
    }

    private static String extractTempPassword(String apiResponseBody) {
        return extractUsingPattern(apiResponseBody, "[.\\w\\S]{0,30}(?==0ASign)");
    }


    private static String extractSignInCode(String emailContent) {
        return extractUsingPattern(emailContent, "[\\d]{6}(?= The code)");
    }


    private static String extractLinkFromEmail(String emailContent) {
        return extractUsingPattern(emailContent, "https?://[\\w\\S]+");
    }


    private static String extractUsernameFromEmail(String emailContent) {
        return extractUsingPattern(emailContent, "[\\w\\S]{0,30}(?==0ASign)");
    }


    private static String extractUsingPattern(String content, String pattern) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(content);
        if (matcher.find()) {
            return matcher.group();
        } else {
            throw new IllegalStateException("Pattern not found in the email content.");
        }
    }


    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("test", "dvsa");
        return headers;
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