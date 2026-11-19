package activesupport.driver.Parallel;

import activesupport.driver.Browser;
import activesupport.proxy.JumpboxProxy;
import activesupport.system.Properties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalHttpBrowserOptionsTest {

    static Stream<Object[]> environmentsAndDrivers() {
        return Stream.of("local", "LOCAL", "qa", null)
                .flatMap(env -> Stream.of(false, true)
                        .flatMap(edge -> Stream.of(false, true)
                                .map(remote -> new Object[]{env, edge, remote})));
    }

    @ParameterizedTest
    @MethodSource("environmentsAndDrivers")
    void suppressesHttpsUpgradesOnlyForLocal(String environment, boolean edge, boolean remote)
            throws Exception {
        try (var properties = mockStatic(Properties.class);
             var browser = mockStatic(Browser.class);
             var proxy = mockStatic(JumpboxProxy.class);
             var chromeDrivers = mockConstruction(ChromeDriver.class);
             var edgeDrivers = mockConstruction(EdgeDriver.class);
             var remoteDrivers = mockConstruction(RemoteWebDriver.class)) {
            properties.when(() -> Properties.get("env")).thenReturn(environment);
            browser.when(Browser::getBrowserVersion).thenReturn(remote ? "latest" : null);
            browser.when(Browser::hubURL).thenReturn("http://localhost:4444/wd/hub");
            browser.when(Browser::getPlatform).thenReturn("LINUX");

            var chromeSetUp = new ChromeSetUp();
            var edgeSetUp = new EdgeSetUp();
            chromeSetUp.getChromeOptions().setExperimentalOption("prefs", Map.of("custom.preference", true));

            var driver = edge ? edgeSetUp.driver() : chromeSetUp.driver();
            var options = edge ? edgeSetUp.getEdgeOptions() : chromeSetUp.getChromeOptions();
            var chromiumOptions = (Map<?, ?>) options.getCapability(edge ? "ms:edgeOptions" : "goog:chromeOptions");
            var arguments = (List<?>) chromiumOptions.get("args");
            var disabledFeatures = arguments.stream().map(Object::toString)
                    .filter(argument -> argument.startsWith("--disable-features="))
                    .toList();
            var prefs = (Map<?, ?>) chromiumOptions.get("prefs");
            boolean local = "local".equalsIgnoreCase(environment);

            assertEquals(local, disabledFeatures.stream().anyMatch(value -> value.contains("HttpsUpgrades")));
            assertEquals(local, disabledFeatures.stream().anyMatch(value -> value.contains("HttpsFirstBalancedModeAutoEnable")));
            assertEquals(local ? false : null, prefs.get("https_only_mode_enabled"));
            assertEquals(local ? false : null, prefs.get("https_upgrades_enabled"));
            if (edge) {
                assertEquals(1, disabledFeatures.size(), "Keep Edge feature switches in a single argument");
                assertTrue(disabledFeatures.getFirst().contains("BackForwardCache"));
                assertEquals(false, prefs.get("safebrowsing.enabled"));
            } else {
                assertEquals(true, prefs.get("custom.preference"));
                assertEquals(local, Boolean.TRUE.equals(options.getCapability("se:downloadsEnabled")));
            }
            assertSame(remote ? remoteDrivers.constructed().getFirst()
                    : edge ? edgeDrivers.constructed().getFirst() : chromeDrivers.constructed().getFirst(), driver);
            if (remote) {
                var remoteDriver = remoteDrivers.constructed().getFirst();
                if (local) {
                    verify(remoteDriver).setFileDetector(isA(LocalFileDetector.class));
                } else {
                    verify(remoteDriver, never()).setFileDetector(any());
                }
            }
        }
    }
}
