package io.jans.as.server.jwk.ws.rs;

import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.orm.PersistenceEntryManager;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class ArchivedJwksServiceTest {

    @InjectMocks
    private ArchivedJwksService archivedJwksService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private PersistenceEntryManager persistenceEntryManager;

    @Mock
    private StaticConfiguration staticConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    public void buildDn_whenCalled_shouldReturnCorrectValue() {
        BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
        baseDnConfiguration.setArchivedJwks("ou=archived_jwks,o=gluu");

        when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);

        assertEquals("jansId=kid,ou=archived_jwks,o=gluu", archivedJwksService.buildDn("kid"));
    }

    @Test
    public void getLifetimeInSeconds_whenConfigIsAbsent_shouldReturnOneYear() {
        assertEquals(ArchivedJwksService.SECONDS_IN_ONE_YEAR, archivedJwksService.getLifetimeInSeconds());
    }

    @Test
    public void getLifetimeInSeconds_whenConfigIsSet_shouldReturnValueFromConfig() {
        when(appConfiguration.getArchivedJwkLifetimeInSeconds()).thenReturn(10);

        assertEquals(10, archivedJwksService.getLifetimeInSeconds());
    }

    @Test
    public void findRemovedKeys_whenCalled_shouldReturnCorrectValues() {
        JSONObject existing = new JSONObject("{\n" +
                "  \"keys\" : [ {\n" +
                "    \"kty\" : \"RSA\",\n" +
                "    \"e\" : \"AQAB\",\n" +
                "    \"use\" : \"sig\",\n" +
                "    \"kid\" : \"b302e936-b14d-4b04-b3b8-c1793f827d9a_sig_rs256\"\n" +
                "  }, {\n" +
                "    \"kid\" : \"3d5bb406-a0ee-447a-babe-2c86df27fb96_sig_rs384\",\n" +
                "    \"exp\" : 1699400350705,\n" +
                "    \"alg\" : \"RS384\"\n" +
                "  }\n" +
                "]\n" +
                "}");
        JSONObject newKeys = new JSONObject("{\n" +
                "  \"keys\" : [ {\n" +
                "    \"kty\" : \"RSA\",\n" +
                "    \"e\" : \"AQAB\",\n" +
                "    \"use\" : \"sig\",\n" +
                "    \"kid\" : \"b302e936-b14d-4b04-b3b8-c1793f827d9a_sig_rs256\"\n" +
                "  }" +
                "]\n" +
                "}");

        final Map<String, JSONObject> removedKeys = archivedJwksService.findRemovedKeys(existing, newKeys);
        final JSONObject jsonObject = removedKeys.get("3d5bb406-a0ee-447a-babe-2c86df27fb96_sig_rs384");
        assertNotNull(jsonObject);
        assertEquals(1, removedKeys.size());
    }
}
