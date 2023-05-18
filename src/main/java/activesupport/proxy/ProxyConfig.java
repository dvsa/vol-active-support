package activesupport.proxy;

import org.openqa.selenium.Proxy;

public class ProxyConfig {
    public static Proxy dvsaProxy() {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(System.getProperty("httpProxy"));
        proxy.setSslProxy(System.getProperty("httpsProxy"));
        proxy.setNoProxy(System.getProperty("noProxy"));
        return proxy;
    }
}