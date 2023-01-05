package io.jans.as.server.par.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import jakarta.ws.rs.WebApplicationException;

import static org.mockito.Mockito.when;

/**
 * @author Yuriy Zabrovarnyy
 */
@Listeners(MockitoTestNGListener.class)
public class ParValidatorTest {

    @InjectMocks
    private ParValidator parValidator;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory; // mock is required

    @SuppressWarnings("java:S5976") // we do want separate methods with clear names. Clarity of use case.
    @Test
    public void validatePkce_whenFapiIsFalse_shouldNotThrowError() {
        when(appConfiguration.isFapi()).thenReturn(false);

        parValidator.validatePkce(null, null, null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validatePkce_whenFapiIsTrueAndNoCodeChallenage_shouldThrowError() {
        when(appConfiguration.isFapi()).thenReturn(true);

        parValidator.validatePkce(null, "s256", null);
    }

    @Test
    public void validatePkce_whenFapiIsTrueAndCodeChallenageIsSet_shouldNotThrowError() {
        when(appConfiguration.isFapi()).thenReturn(true);

        parValidator.validatePkce("codechallangehere", "s256",null);
    }

}
