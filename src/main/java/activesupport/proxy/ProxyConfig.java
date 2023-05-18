package activesupport.proxy;

import org.openqa.selenium.Proxy;

public class ProxyConfig {
    public static Proxy dvsaProxy() {
        Proxy proxy = new Proxy();
        proxy.setSslProxy(System.getProperty("httpProxy"));
        return proxy;
    }
}