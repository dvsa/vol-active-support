package activesupport.proxy;

import org.openqa.selenium.Proxy;

import java.util.ArrayList;
import java.util.List;

public class ProxyConfig {
    public static List<String> ignoreCertErrors() {
        List<String> chromeSwitches = new ArrayList<>();
        chromeSwitches.add("--ignore-certificate-errors");
        chromeSwitches.add("--allow-running-insecure-content");
        chromeSwitches.add("--disable-gpu");
        return chromeSwitches;
    }

    public static Proxy dvsaProxy() {
        Proxy proxy = new Proxy();
        proxy.setAutodetect(false);
        proxy.setSslProxy(System.getProperty("httpsProxy"));
        proxy.setHttpProxy(System.getProperty("httpProxy"));
        proxy.setNoProxy(System.getProperty("noProxy"));
        return proxy;
    }
}