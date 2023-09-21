package io.jans.as.server.register.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.ciba.CIBARegisterClientResponseService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ScopeService;
import io.jans.util.security.StringEncrypter;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class RegisterJsonServiceTest {

    @InjectMocks
    @Spy
    private RegisterJsonService registerJsonService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ClientService clientService;

    @Mock
    private ScopeService scopeService;

    @Mock
    private AttributeService attributeService;

    @Mock
    private CIBARegisterClientResponseService cibaRegisterClientResponseService;

    @Test
    public void getJSONObject_whenOrgIdSet_shouldHaveOrgIdPresentInJson() throws StringEncrypter.EncryptionException {
        Client client = new Client();
        client.setOrganization("testOrgId");
        client.setClientIdIssuedAt(new Date());

        final JSONObject json = registerJsonService.getJSONObject(client);
        assertEquals(json.get("org_id"), "testOrgId");
    }

    @Test
    public void getJSONObject_whenOrgIdIsNotSet_shouldHaveNullOrgIdInJson() throws StringEncrypter.EncryptionException {
        Client client = new Client();
        client.setOrganization(null);
        client.setClientIdIssuedAt(new Date());

        final JSONObject json = registerJsonService.getJSONObject(client);
        assertNull(json.opt("org_id"));
    }
}
