package activesupport;

public class IllegalBrowserException extends Exception {
    public IllegalBrowserException(String browserNameCannotBeNull) {
        super("[ERROR] incorrect browser name." + "/n" + "[OPTIONS] 1) chrome, 2) edge, 3) firefox, 4) safari, 5) headless, 6) chrome-proxy, 7)firefox-proxy ");
    }
}