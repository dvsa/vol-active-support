package activesupport.driver;

import activesupport.aws.s3.SecretsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.AddInterceptParameters;
import org.openqa.selenium.bidi.network.InterceptPhase;
import org.openqa.selenium.remote.Augmenter;

/**
 * Answers HTTP basic auth challenges at the browser's network layer.
 *
 * <p>Credentials embedded in a URL only authenticate the one navigation the test issues. They
 * cannot cover a request the browser makes on its own, and an external identity provider
 * redirecting back to the service is exactly that: the browser follows the 302 unaided, the request
 * carries no credentials, and the callback gets a 401 instead of rendering.
 *
 * <p>A BiDi intercept is attached to the browsing context rather than to a navigation, so it fires
 * for every request the browser makes, redirect hops included.
 *
 * <p>Registration is per thread because the driver is thread local and scenarios run in parallel.
 */
public final class BasicAuth {

    private static final Logger LOGGER = LogManager.getLogger(BasicAuth.class);

    private static final ThreadLocal<Network> NETWORK = new ThreadLocal<>();

    private BasicAuth() {
    }

    /**
     * Prepares a freshly created driver: exposes BiDi where it is available, then registers the
     * basic auth handler against it.
     *
     * <p>A RemoteWebDriver does not implement HasBiDi on its own, so it is augmented first. That
     * returns a new instance, which is why callers must store what comes back rather than the
     * driver they passed in.
     *
     * @return the driver to use from here on, augmented where that was possible
     */
    public static WebDriver enableFor(WebDriver driver) {
        if (driver == null) {
            return null;
        }

        if (!(driver instanceof HasBiDi)) {
            driver = augment(driver);
        }

        register(driver);
        return driver;
    }

    public static void clear() {
        Network network = NETWORK.get();

        if (network == null) {
            return;
        }

        try {
            network.close();
        } catch (Exception e) {
            LOGGER.warn("Could not close the basic auth handler: {}", e.getMessage());
        } finally {
            NETWORK.remove();
        }
    }

    private static WebDriver augment(WebDriver driver) {
        try {
            WebDriver augmented = new Augmenter().augment(driver);

            if (augmented instanceof HasBiDi) {
                return augmented;
            }

            // Firefox does not set the webSocketUrl capability, so there is nothing to augment to.
            LOGGER.warn("Driver does not expose BiDi, so basic auth challenges cannot be answered");
        } catch (Exception e) {
            LOGGER.warn("Could not augment the driver for BiDi: {}", e.getMessage());
        }

        return driver;
    }

    private static void register(WebDriver driver) {
        if (!(driver instanceof HasBiDi) || NETWORK.get() != null) {
            return;
        }

        String userName = secret("basicAuthUserName");
        String password = secret("basicAuthPassword");

        if (userName == null || password == null) {
            LOGGER.info("No basic auth credentials configured, so no handler was registered");
            return;
        }

        try {
            Network network = new Network(driver);
            network.addIntercept(new AddInterceptParameters(InterceptPhase.AUTH_REQUIRED));
            network.onAuthRequired(details -> network.continueWithAuth(
                    details.getRequest().getRequestId(),
                    new UsernameAndPassword(userName, password)));

            NETWORK.set(network);
            LOGGER.info("Registered basic auth handler for the current browser session");
        } catch (Exception e) {
            LOGGER.warn("Could not register the basic auth handler: {}", e.getMessage());
        }
    }

    private static String secret(String key) {
        try {
            String value = SecretsManager.getSecretValue(key);
            return value == null || value.isBlank() ? null : value;
        } catch (Exception e) {
            LOGGER.warn("Could not read {} from secrets manager: {}", key, e.getMessage());
            return null;
        }
    }
}
