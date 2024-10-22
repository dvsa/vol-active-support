package activesupport.proxy;

import activesupport.aws.s3.SecretsManager;
import org.openqa.selenium.Proxy;

public class ProxyConfig {
    public static Proxy dvsaProxy() {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(SecretsManager.getSecretValue("httpProxy"));
        proxy.setSslProxy(SecretsManager.getSecretValue("httpsProxy"));
        proxy.setNoProxy(System.getProperty("noProxy"));
        System.out.println(proxy.getHttpProxy());
        System.out.println(proxy.getSslProxy());
        System.out.println(proxy.getNoProxy());
        return proxy;
    }
}