package io.jans.as.server.auth;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.service.CacheService;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

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
    public void validateDpopNonce_whenUseDpopNonceIsFalse_shouldSkipValidation() {
        when(appConfiguration.getDpopUseNonce()).thenReturn(false);

        dpopService.validateDpopNonce(null);
        dpopService.validateDpopNonce("");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateDpopNonce_whenUseDpopNonceIsTrueAndNoNonce_shouldFail() {
        when(appConfiguration.getDpopUseNonce()).thenReturn(true);

        dpopService.validateDpopNonce(null);
    }

    @Test
    public void validateDpopNonce_whenUseDpopNonceIsTrueAndHasValidNonce_shouldPass() {
        when(appConfiguration.getDpopUseNonce()).thenReturn(true);

        final String nonce = UUID.randomUUID().toString();
        when(cacheService.get(nonce)).thenReturn(nonce);

        dpopService.validateDpopNonce(nonce);
    }

    @Test
    public void validateDpopNonce_whenUseDpopNonceIsTrueAndHasInvalidNonce_shouldFail() {
        when(appConfiguration.getDpopUseNonce()).thenReturn(true);

        try {
            dpopService.validateDpopNonce("fake");
        } catch (WebApplicationException e) {
            final String newNonce = e.getResponse().getHeaderString(DpopService.DPOP_NONCE);
            assertTrue(StringUtils.isNotBlank(newNonce));
            return;
        }

        fail("No error with DPoP-Nonce header.");
    }

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
