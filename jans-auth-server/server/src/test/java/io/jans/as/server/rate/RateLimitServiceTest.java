package io.jans.as.server.rate;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.register.ws.rs.RegisterService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class RateLimitServiceTest {

    @InjectMocks
    @Spy
    private RateLimitService rateLimitService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private RegisterService registerService;

    @Test
    public void validateRateLimitForRegister_forSingleCall_shouldPassSuccessfully() throws RateLimitedException {
        rateLimitService.validateRateLimitForRegister("some_ssa");
    }

    @Test
    public void validateRateLimitForRegister_forManyCallOverRateLimit_shouldFail() {
        try {
            when(appConfiguration.getRateLimitRegistrationRequestCount()).thenReturn(3);
            when(appConfiguration.getRateLimitRegistrationPeriodInSeconds()).thenReturn(40);

            for (int i = 0; i < 10; i++) {
                System.out.println("validateRateLimitForRegister - " + i);
                rateLimitService.validateRateLimitForRegister("some_ssa");
            }
        } catch (RateLimitedException e) {
            System.out.println("validateRateLimitForRegister - rate limit exception. Passed successfully.");
            return;
        }

        Assert.fail("Rate limit exception was not thrown. But it's expected to get it.");
    }
}
