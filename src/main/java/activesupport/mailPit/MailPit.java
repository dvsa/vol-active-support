
package activesupport.mailPit;

import activesupport.MissingRequiredArgument;
import activesupport.http.RestUtils;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailPit {
    private static final Semaphore rateLimiter = new Semaphore(3);

    private static final Logger LOGGER = LogManager.getLogger(MailPit.class);
    private static final int ACQUIRE_TIMEOUT = 30;
    private volatile ValidatableResponse response;
    private String ip;
    private String port;
    private static final int MAX_RETRIES = 10;

    public MailPit() {
        this.ip = "https://selenium-mail.olcs.dev-dvsacloud.uk:8025";
        this.port = "8025";
    }

    public String retrieveTempPassword(String emailAddress) {
        try {
            LOGGER.info("Attempting to acquire rate limiter permit");
            if (!rateLimiter.tryAcquire(30L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout waiting for rate limiter permit");
            } else {
                try {
                    int attempts = 0;
                    IllegalStateException lastException = null;

                    while (attempts < 10) {
                        try {
                            LOGGER.info("Attempt {}: Sleeping for 3 seconds before making request", attempts + 1);
                            TimeUnit.SECONDS.sleep(3L);
                            ++attempts;
                            Map<String, String> queryParams = new HashMap<>();
                            queryParams.put("q", emailAddress + " : Your temporary password");
                            String url = String.format("%s/api/v1/messages", this.getIp());
                            LOGGER.info("Making request to URL: {}", url);
                            this.response = RestUtils.getWithQueryParams(url, queryParams, this.getHeaders());
                            String responseBody = this.response.extract().asString();
                            LOGGER.info("Response received: {}", responseBody);
                            if (!StringUtils.isEmpty(responseBody) && responseBody.contains("messages")) {
                                JsonPath jsonPath = new JsonPath(responseBody);
                                if (jsonPath.getList("messages") != null && !jsonPath.getList("messages").isEmpty()) {
                                    String snippetPath = "messages.find { msg -> msg.Subject.startsWith('" + emailAddress + "') && msg.Subject.contains('temporary password') }.Snippet";
                                    String snippet = jsonPath.getString(snippetPath);
                                    LOGGER.info("Snippet found: {}", snippet);
                                    if (snippet != null) {
                                        String rawPassword = extractRawPassword(snippet);
                                        String var11 = prepareForQuotedPrintable(rawPassword);
                                        LOGGER.info("Password retrieved successfully");
                                        return var11;
                                    }
                                }
                            }
                        } catch (Exception var16) {
                            LOGGER.error("Error processing response on attempt {}: {}", attempts, var16.getMessage());
                            lastException = new IllegalStateException("Error processing response: " + var16.getMessage(), var16);
                        }
                    }

                    throw new IllegalStateException("Failed to retrieve password after 10 attempts for " + emailAddress, lastException);
                } finally {
                    LOGGER.info("Releasing rate limiter permit");
                    rateLimiter.release();
                }
            }
        } catch (InterruptedException var18) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate limiter", var18);
        }
    }

    private static String extractRawPassword(String apiResponseBody) {
        Pattern pattern = Pattern.compile("is:\\s*([^\\s]+)");
        Matcher matcher = pattern.matcher(apiResponseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Password pattern not found in email content");
    }

    private static String prepareForQuotedPrintable(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("Empty password");
        }
        password = password.replaceAll("=\\r\\n", "")
                .replaceAll("=\\n", "")
                .replaceAll("=0D", "\r")
                .replaceAll("=0A", "\n");
        StringBuilder result = new StringBuilder();
        for (char c : password.toCharArray()) {
            if (c >= 33 && c <= 126 && c != '=' && c != '?' && c != '_' && c != '~') {
                result.append(c);
            } else {
                result.append(String.format("=%02X", (int) c));
            }
        }
        return result.toString();
    }

    public String retrieveEmailContent(String emailAddress, String subjectContains) {
        try {
            if (!rateLimiter.tryAcquire(ACQUIRE_TIMEOUT, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout waiting for rate limiter permit");
            }
            try {
                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("q", emailAddress + " : " + subjectContains);
                String url = String.format("%s/api/v1/messages", this.getIp());
                response = RestUtils.getWithQueryParams(url, queryParams, this.getHeaders());
                String responseBody = this.response.extract().asString();
                if (StringUtils.isNotEmpty(responseBody)) {
                    JsonPath jsonPath = new JsonPath(responseBody);
                    return jsonPath.getString("messages[0].Snippet");
                }
                return null;
            } finally {
                rateLimiter.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate limiter", e);
        }
    }

    public String retrieveSignInCode(String emailAddress) {
        String emailContent = retrieveEmailContent(emailAddress, "Your sign-in code");
        Pattern pattern = Pattern.compile("([\\d]{6})(?= The code)");
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Sign-in code not found in email");
    }

    public String retrieveTmAppLink(String emailAddress) throws MissingRequiredArgument {
        String emailContent = retrieveEmailContent(emailAddress, "A Transport Manager has submitted their details for review");
        Pattern pattern = Pattern.compile("(https?://[\\w\\S]+)");
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("TM App link not found in email");
    }

    public String retrievePasswordResetLink(@NotNull String emailAddress) throws MissingRequiredArgument {
        try {
            TimeUnit.SECONDS.sleep(10);
            String emailContent = retrieveEmailContent(emailAddress, "Reset your password");
            Pattern pattern = Pattern.compile("(https?://[\\w\\S]+)");
            Matcher matcher = pattern.matcher(emailContent);
            if (matcher.find()) {
                return matcher.group(1);
            }
            throw new IllegalStateException("Password reset link not found in email");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public String retrieveUsernameInfo(String emailAddress) throws MissingRequiredArgument {
        String emailContent = retrieveEmailContent(emailAddress, "Your account information");
        Pattern pattern = Pattern.compile("username is: ([\\w\\S]+)");
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Username not found in email");
    }

    public boolean checkLastTMLetterAttachment(@NotNull String emailAddress, String licenceNo) throws MissingRequiredArgument {
        String emailContent = retrieveEmailContent(emailAddress, "Urgent Removal of last Transport Manager");
        return emailContent.contains(String.format("%s_Last_TM_letter_Licence_%s", licenceNo, licenceNo));
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("test", "dvsa");
        headers.put("Accept", "application/json");
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