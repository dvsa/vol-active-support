package activesupport.mailPit;

import activesupport.MissingRequiredArgument;
import activesupport.http.RestUtils;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailPit {
    private static final Semaphore rateLimiter = new Semaphore(10);
    private static final Logger LOGGER = LogManager.getLogger(MailPit.class);
    private static final int ACQUIRE_TIMEOUT = 60;
    private static final int DEFAULT_TIME_WINDOW_MINUTES = 5;
    private volatile ValidatableResponse response;
    private String ip;
    private String port;
    private static final int MAX_RETRIES = 15;

    public MailPit() {
        this.ip = "https://selenium-mail.olcs.dev-dvsacloud.uk:8025";
        this.port = "8025";
    }

    public String retrieveTempPassword(String emailAddress) {
        return retrieveTempPassword(emailAddress, DEFAULT_TIME_WINDOW_MINUTES);
    }


    public String retrieveTempPassword(String emailAddress, int timeWindowMinutes) {
    try {
        LOGGER.info("Attempting to acquire rate limiter permit");
        if (!rateLimiter.tryAcquire(30L, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timeout waiting for rate limiter permit");
        } else {
            try {
                int attempts = 0;
                IllegalStateException lastException = null;
                Random random = new Random();

                while (attempts < 10) {
                    try {
                        long baseDelay = 3000L + (attempts * 1000L);
                        long jitter = random.nextInt(2000);
                        long totalDelay = baseDelay + jitter;
                        
                        LOGGER.info("Attempt {}: Sleeping for {} ms before making request", attempts + 1, totalDelay);
                        TimeUnit.MILLISECONDS.sleep(totalDelay);
                        ++attempts;

                        Map<String, String> queryParams = createTimeFilteredQuery(emailAddress, Math.max(timeWindowMinutes, 5));
                        String url = String.format("%s/api/v1/search", this.getIp());
                        LOGGER.info("Making request to URL: {} with time-filtered query", url);

                        this.response = RestUtils.getWithQueryParams(url, queryParams, this.getHeaders());
                        String responseBody = this.response.extract().asString();
                        LOGGER.info("Response received with {} characters", responseBody.length());

                        if (!StringUtils.isEmpty(responseBody) && responseBody.contains("messages")) {
                            JsonPath jsonPath = new JsonPath(responseBody);
                            List<Map<String, Object>> messages = jsonPath.getList("messages");

                            if (messages == null || messages.isEmpty()) {
                                LOGGER.warn("No messages found, waiting longer before retry");
                                TimeUnit.SECONDS.sleep(5L);
                                continue;
                            }

                            LOGGER.info("Found {} recent messages", messages.size());

                            List<Map<String, Object>> recentMessages = filterMessagesByTime(messages, Math.max(timeWindowMinutes, 5));
                            LOGGER.info("Found {} messages within {} minutes", recentMessages.size(), Math.max(timeWindowMinutes, 5));

                            for (Map<String, Object> message : recentMessages) {
                                String subject = (String) message.get("Subject");
                                String snippet = (String) message.get("Snippet");
                                String created = (String) message.get("Created");

                                LOGGER.info("Checking message - Subject: '{}', Created: {}", subject, created);

                                if (subject != null &&
                                        subject.contains(emailAddress) &&
                                        subject.toLowerCase().contains("temporary password")) {

                                    if (snippet != null && snippet.toLowerCase().contains("temporary password")) {
                                        // Get message ID and fetch full message details
                                        String messageId = (String) message.get("ID");
                                        LOGGER.info("Found matching message ID: {} with snippet: {}", messageId, snippet);
                                        
                                        // Fetch full message details to get the Text property
                                        String messageUrl = String.format("%s/api/v1/message/%s", this.getIp(), messageId);
                                        LOGGER.info("Fetching message details from: {}", messageUrl);

                                        this.response = RestUtils.get(messageUrl, this.getHeaders());
                                        String messageResponseBody = this.response.extract().asString();
                                        JsonPath messageJsonPath = new JsonPath(messageResponseBody);
                                        String textContent = messageJsonPath.getString("Text");
                                        
                                        if (textContent != null) {
                                            LOGGER.debug("Text content retrieved, length: {} characters", textContent.length());
                                            
                                            // Extract password from clean text content
                                            // Pattern matches: "account is: PASSWORD" followed by period+newline or just newline
                                            Pattern pattern = Pattern.compile(
                                                "account is: ([^\\n]+?)(?:\\.?\\n)",
                                                Pattern.DOTALL
                                            );
                                            
                                            Matcher matcher = pattern.matcher(textContent);
                                            if (matcher.find()) {
                                                String password = matcher.group(1).trim();
                                                LOGGER.info("Password extracted from text content: {}", password);
                                                return password;
                                            }
                                        }
                                        
                                        // Fallback to existing extraction method
                                        LOGGER.warn("Failed to extract from text content, trying snippet method");
                                        String rawPassword = extractRawPassword(snippet);
                                        String processedPassword = prepareForQuotedPrintable(rawPassword);
                                        LOGGER.info("Password retrieved using fallback method: {}", rawPassword);
                                        return processedPassword;
                                    }
                                }
                            }

                            LOGGER.warn("No matching message found for email: {}", emailAddress);
                            LOGGER.warn("Recent subjects found:");
                            for (Map<String, Object> message : recentMessages) {
                                String subject = (String) message.get("Subject");
                                String created = (String) message.get("Created");
                                LOGGER.warn("  - '{}' ({})", subject, created);
                            }
                        } else {
                            LOGGER.warn("Response body is empty or doesn't contain 'messages'");
                        }
                    } catch (Exception var16) {
                        LOGGER.error("Error processing response on attempt {}: {}", attempts, var16.getMessage(), var16);
                        lastException = new IllegalStateException("Error processing response: " + var16.getMessage(), var16);
                        
                        if (attempts < 9) {
                            long retryDelay = 2000L + (attempts * 1000L);
                            try {
                                TimeUnit.MILLISECONDS.sleep(retryDelay);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("Interrupted during retry delay", ie);
                            }
                        }
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
    private Map<String, String> createTimeFilteredQuery(String emailAddress, int timeWindowMinutes) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("query", "subject:\"" + emailAddress + "\"");
        queryParams.put("limit", "50");
        Instant cutoffTime = Instant.now().minus(timeWindowMinutes, ChronoUnit.MINUTES);
        String since = cutoffTime.toString();
        queryParams.put("since", since);
        LOGGER.info("Time-filtered query: email={}, limit=50, since={}", emailAddress, since);
        return queryParams;
    }


    private List<Map<String, Object>> filterMessagesByTime(List<Map<String, Object>> messages, int timeWindowMinutes) {
        List<Map<String, Object>> filteredMessages = new ArrayList<>();
        Instant cutoffTime = Instant.now().minus(timeWindowMinutes, ChronoUnit.MINUTES);

        for (Map<String, Object> message : messages) {
            String createdStr = (String) message.get("Created");
            if (createdStr != null) {
                try {
                    Instant messageTime = Instant.parse(createdStr);
                    if (messageTime.isAfter(cutoffTime)) {
                        filteredMessages.add(message);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse message time: {}", createdStr, e);
                    filteredMessages.add(message);
                }
            }
        }

        return filteredMessages;
    }

    public String retrieveEmailContent(String emailAddress, String subjectContains, int timeWindowMinutes) {
        try {
            if (!rateLimiter.tryAcquire(ACQUIRE_TIMEOUT, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout waiting for rate limiter permit");
            }
            try {
                Map<String, String> queryParams = createTimeFilteredQuery(emailAddress, timeWindowMinutes);
                String url = String.format("%s/api/v1/search", this.getIp());
                response = RestUtils.getWithQueryParams(url, queryParams, this.getHeaders());
                String responseBody = this.response.extract().asString();

                if (StringUtils.isNotEmpty(responseBody)) {
                    JsonPath jsonPath = new JsonPath(responseBody);
                    List<Map<String, Object>> messages = jsonPath.getList("messages");

                    if (messages != null) {
                        List<Map<String, Object>> recentMessages = filterMessagesByTime(messages, timeWindowMinutes);

                        for (Map<String, Object> message : recentMessages) {
                            String subject = (String) message.get("Subject");
                            if (subject != null && subject.contains(subjectContains)) {
                                return (String) message.get("Snippet");
                            }
                        }
                    }
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

    public String retrieveEmailContent(String emailAddress, String subjectContains) {
        return retrieveEmailContent(emailAddress, subjectContains, DEFAULT_TIME_WINDOW_MINUTES);
    }


    public synchronized String retrieveEmailRawContent(String emailAddress, String subjectContains, int timeWindowMinutes) {
        try {
            if (!rateLimiter.tryAcquire(30L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout waiting for rate limiter permit");
            } else {
                try {
                    Map<String, String> queryParams = createTimeFilteredQuery(emailAddress, timeWindowMinutes);
                    String searchUrl = String.format("%s/api/v1/search", this.getIp());
                    LOGGER.info("Search URL: {}", searchUrl);

                    this.response = RestUtils.getWithQueryParams(searchUrl, queryParams, this.getHeaders());
                    String responseBody = this.response.extract().asString();

                    if (StringUtils.isNotEmpty(responseBody)) {
                        JsonPath jsonPath = new JsonPath(responseBody);
                        List<Map<String, Object>> messages = jsonPath.getList("messages");
                        List<Map<String, Object>> recentMessages = filterMessagesByTime(messages, timeWindowMinutes);

                        for (Map<String, Object> message : recentMessages) {
                            String subject = (String) message.get("Subject");
                            if (subject != null && subject.contains(subjectContains)) {
                                String messageId = (String) message.get("ID");
                                LOGGER.info("Found message ID: {}", messageId);
                                String rawUrl = String.format("%s/api/v1/message/%s/raw", this.getIp(), messageId);
                                LOGGER.info("Raw content URL: {}", rawUrl);
                                this.response = RestUtils.get(rawUrl, this.getHeaders());
                                return this.response.extract().asString();
                            }
                        }
                    }
                } finally {
                    rateLimiter.release();
                }
                throw new IllegalStateException("Email content not found for the specified email address and subject.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate limiter", e);
        }
    }

    public synchronized String retrieveEmailRawContent(String emailAddress, String subjectContains) {
        return retrieveEmailRawContent(emailAddress, subjectContains, DEFAULT_TIME_WINDOW_MINUTES);
    }

    public String retrieveSignInCode(String emailAddress, int timeWindowMinutes) {
        String emailContent = retrieveEmailContent(emailAddress, "Your sign-in code", timeWindowMinutes);
        Pattern pattern = Pattern.compile("([\\d]{6})(?= The code)");
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Sign-in code not found in email");
    }

    public String retrieveSignInCode(String emailAddress) {
        return retrieveSignInCode(emailAddress, DEFAULT_TIME_WINDOW_MINUTES);
    }

    private static String extractRawPassword(String apiResponseBody) {
        String[] patterns = {
                "is:\\s*([^\\s\\n\\r]+)",
                "is:\\s*([^\\s,\\n\\r]+)",
                "password.*?is:\\s*([^\\s\\n\\r]+)",
                "account is:\\s*([^\\s\\n\\r]+)"
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(apiResponseBody);
            if (matcher.find()) {
                String password = matcher.group(1);
                LOGGER.info("Password found using pattern '{}': {}", patternStr, password);
                return password;
            }
        }

        LOGGER.error("Password pattern not found in email content: {}", apiResponseBody);
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

    public String retrieveTmAppLink(String emailAddress) throws MissingRequiredArgument {
        return retrieveTmAppLink(emailAddress, DEFAULT_TIME_WINDOW_MINUTES);
    }

    public String retrieveTmAppLink(String emailAddress, int timeWindowMinutes) throws MissingRequiredArgument {
        int retries = 0;
        while (retries < 2) {
            try {
                String emailContent = retrieveEmailRawContent(emailAddress, "A Transport Manager has submitted their details for review", timeWindowMinutes);
                if (emailContent == null) {
                    throw new IllegalStateException("Email content not found");
                }
                if (!emailContent.contains(emailAddress)) {
                    throw new IllegalStateException("Email content does not match the expected user: " + emailAddress);
                }
                return new Scanner(emailContent).useDelimiter("\\A").next();
            } catch (IllegalStateException e) {
                LOGGER.warn("Attempt {} failed: {}. Retrying... ({}/{})", retries + 1, e.getMessage(), retries + 1, 2);
            }
            retries++;
        }
        throw new IllegalStateException("Failed to retrieve TM application link after 2 retries for user: " + emailAddress);
    }

    public String retrievePasswordResetLink(@NotNull String emailAddress, long sleepTime) throws MissingRequiredArgument {
        return retrievePasswordResetLink(emailAddress, sleepTime, DEFAULT_TIME_WINDOW_MINUTES);
    }

    public String retrievePasswordResetLink(@NotNull String emailAddress, long sleepTime, int timeWindowMinutes) throws MissingRequiredArgument {
        int retries = 0;
        while (retries < 2) {
            try {
                TimeUnit.SECONDS.sleep(sleepTime);
                String emailContent = retrieveEmailRawContent(emailAddress, "Reset your password", timeWindowMinutes);
                if (emailContent.contains(emailAddress)) {
                    Pattern pattern = Pattern.compile("href=3D\"([^\"]+)");
                    Matcher matcher = pattern.matcher(emailContent);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                    throw new IllegalStateException("Password reset link not found in email");
                } else {
                    LOGGER.warn("Email content does not match the expected user: {}. Retrying... ({}/{})", emailAddress, retries + 1, 2);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Thread interrupted while retrieving password reset link for user: {}", emailAddress, e);
                return null;
            }
            retries++;
        }
        throw new IllegalStateException("Failed to retrieve password reset link after 2 retries for user: " + emailAddress);
    }

    public String retrieveUsernameInfo(String emailAddress) throws MissingRequiredArgument {
        return retrieveUsernameInfo(emailAddress, DEFAULT_TIME_WINDOW_MINUTES);
    }

    public String retrieveUsernameInfo(String emailAddress, int timeWindowMinutes) throws MissingRequiredArgument {
        String emailContent = retrieveEmailRawContent(emailAddress, "Your account information", timeWindowMinutes);
        Pattern pattern = Pattern.compile("It=E2=80=99s:\\s*([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Username not found in email");
    }

    public boolean checkLastTMLetterAttachment(@NotNull String emailAddress, String licenceNo) throws MissingRequiredArgument {
        return checkLastTMLetterAttachment(emailAddress, licenceNo, DEFAULT_TIME_WINDOW_MINUTES);
    }

    public boolean checkLastTMLetterAttachment(@NotNull String emailAddress, String licenceNo, int timeWindowMinutes) throws MissingRequiredArgument {
        String emailContent = retrieveEmailContent(emailAddress, "Urgent Removal of last Transport Manager", timeWindowMinutes);
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
