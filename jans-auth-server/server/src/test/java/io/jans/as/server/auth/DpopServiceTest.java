package io.jans.as.server.auth;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.service.CacheService;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class DpopServiceTest {

    @InjectMocks
    private DpopService dpopService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private CacheService cacheService;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Test
    public void validateDpopThumprint_whenExistingDpopThumprintIsMissedAndConfIsFalse_shouldBeValid() {
        when(appConfiguration.getDpopJktForceForAuthorizationCode()).thenReturn(false);

        dpopService.validateDpopThumprint(null, "any");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateDpopThumprint_whenExistingDpopThumprintIsMissedAndConfIsTrue_shouldRaiseException() {
        when(appConfiguration.getDpopJktForceForAuthorizationCode()).thenReturn(true);

        dpopService.validateDpopThumprint(null, "any");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateDpopThumprint_whenExistingDpopThumprintDoesNotMatchToActual_shouldRaiseException() {
        when(appConfiguration.getDpopJktForceForAuthorizationCode()).thenReturn(true);

        dpopService.validateDpopThumprint("existing", "request");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateDpopThumprint_whenExistingDpopThumprintDoesNotMatchToActualAndConfIsFalse_shouldRaiseException() {
        when(appConfiguration.getDpopJktForceForAuthorizationCode()).thenReturn(false);

        dpopService.validateDpopThumprint("existing", "request");
    }

    @Test
    public void validateDpopThumprint_whenExistingDpopThumprintMatchToActual_shouldPass() {
        dpopService.validateDpopThumprint("test", "test");
    }

    @Test
    public void validateDpopThumprintIsPresent_whenDpopIsPresent_shouldPass() {
        when(appConfiguration.getDpopJktForceForAuthorizationCode()).thenReturn(true);

        dpopService.validateDpopThumprintIsPresent("any", "state");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateDpopThumprintIsPresent_whenDpopIsPresent_shouldRaiseException() {
        when(appConfiguration.getDpopJktForceForAuthorizationCode()).thenReturn(true);

        dpopService.validateDpopThumprintIsPresent(null, "state");
    }

    @Test
    public void validateDpopValuesCount_whenValueIsOne_shouldPass() {
        dpopService.validateDpopValuesCount(new String[]{"one"});
    }

    @Test
    public void validateDpopValuesCount_whenValueIsNull_shouldPass() {
        dpopService.validateDpopValuesCount((String[]) null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateDpopValuesCount_whenValuesAreTwo_shouldRaiseException() {
        dpopService.validateDpopValuesCount(new String[]{"one", "two"});
    }
}
