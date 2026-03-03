package io.jans.as.server.service.token;

import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.cluster.StatusIndexPoolService;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(MockitoTestNGListener.class)
public class StatusListServiceTest {

    @InjectMocks
    private StatusListService statusListService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private StatusIndexPoolService statusTokenPoolService;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateTime_whenTimeIsNotEmpty_shouldThrowException() {
        statusListService.validateTime("123");
    }

    @Test
    public void validateTime_whenTimeIsEmpty_shouldPass() {
        statusListService.validateTime(null);
    }
}
