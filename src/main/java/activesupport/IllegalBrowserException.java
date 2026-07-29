package activesupport;

public class IllegalBrowserException extends Exception {
    public IllegalBrowserException() {
        super("[ERROR] incorrect browser name." + "/n" + "[OPTIONS] 1) chrome, 2) chrome-headless (headed alias), 3) edge, 4) firefox, 5) safari, 6) headless (headed alias), 7) chrome-proxy, 8) firefox-proxy ");
    }
}