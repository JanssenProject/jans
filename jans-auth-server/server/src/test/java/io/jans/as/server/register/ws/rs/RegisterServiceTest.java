package io.jans.as.server.register.ws.rs;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.ciba.CIBARegisterClientMetadataService;
import io.jans.as.server.service.ScopeService;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class RegisterServiceTest {

    @InjectMocks
    @Spy
    private RegisterService registerService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Logger log;

    @Mock
    private ScopeService scopeService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AttributeService attributeService;

    @Mock
    private CIBARegisterClientMetadataService cibaRegisterClientMetadataService;

    @Test
    public void addDefaultCustomAttributes_whenCalledWithExistingConfigurationData_shouldPopulateRequestObject() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree("{\"jansInclClaimsInIdTkn\": true, \"jansTrustedClnt\": true}");
        when(appConfiguration.getDynamicRegistrationCustomAttributes()).thenReturn(Lists.newArrayList("jansInclClaimsInIdTkn", "jansTrustedClnt"));
        when(appConfiguration.getDynamicRegistrationDefaultCustomAttributes()).thenReturn(node);

        JSONObject requestObject = new JSONObject();
        registerService.addDefaultCustomAttributes(requestObject);

        assertTrue(requestObject.getBoolean("jansInclClaimsInIdTkn"));
        assertTrue(requestObject.getBoolean("jansTrustedClnt"));
    }

    @Test
    public void addDefaultCustomAttributes_whenCustomAttributePropertyIsEmpty_shouldNotTakeEffect() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree("{\"jansInclClaimsInIdTkn\": true, \"jansTrustedClnt\": true}");
        when(appConfiguration.getDynamicRegistrationCustomAttributes()).thenReturn(Lists.newArrayList());
        when(appConfiguration.getDynamicRegistrationDefaultCustomAttributes()).thenReturn(node);

        JSONObject requestObject = new JSONObject();
        registerService.addDefaultCustomAttributes(requestObject);

        assertTrue(requestObject.isEmpty());
    }

    @Test
    public void addDefaultCustomAttributes_whenCalledWithNoData_shouldNotTakeEffect() {
        JSONObject requestObject = new JSONObject();
        registerService.addDefaultCustomAttributes(requestObject);

        assertTrue(requestObject.isEmpty());
    }
}
