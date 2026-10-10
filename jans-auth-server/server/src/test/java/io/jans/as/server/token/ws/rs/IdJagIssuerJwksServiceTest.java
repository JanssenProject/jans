package io.jans.as.server.token.ws.rs;

import io.jans.as.model.configuration.TrustedIssuerConfig;
import io.jans.as.server.service.net.UriService;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class IdJagIssuerJwksServiceTest {

    private static final String JWKS_URI = "https://idp.example.com/jwks";
    private static final String INLINE_JWKS = "{\"keys\":[{\"kid\":\"inline\"}]}";
    private static final String REMOTE_JWKS = "{\"keys\":[{\"kid\":\"remote\"}]}";

    @Spy
    @InjectMocks
    private IdJagIssuerJwksService service;

    @Mock
    private Logger log;

    @Mock
    private UriService uriService;

    @Test
    public void hasKeySource_whenNullConfig_shouldReturnFalse() {
        assertFalse(service.hasKeySource(null));
    }

    @Test
    public void hasKeySource_whenNeitherJwksNorUri_shouldReturnFalse() {
        assertFalse(service.hasKeySource(new TrustedIssuerConfig()));
    }

    @Test
    public void hasKeySource_whenJwksSet_shouldReturnTrue() {
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwks(INLINE_JWKS);
        assertTrue(service.hasKeySource(config));
    }

    @Test
    public void hasKeySource_whenJwksUriSet_shouldReturnTrue() {
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);
        assertTrue(service.hasKeySource(config));
    }

    @Test
    public void getJwks_whenNullConfig_shouldReturnNull() {
        assertNull(service.getJwks(null, "kid"));
    }

    @Test
    public void getJwks_whenInlineJwks_shouldReturnItWithoutFetching() {
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwks(INLINE_JWKS);

        JSONObject result = service.getJwks(config, "inline");

        assertEquals(result.getJSONArray("keys").getJSONObject(0).getString("kid"), "inline");
        verifyNoInteractions(uriService);
    }

    @Test
    public void getJwks_whenInlineJwksAndUri_shouldPreferInline() {
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwks(INLINE_JWKS);
        config.setJwksUri(JWKS_URI);

        JSONObject result = service.getJwks(config, "inline");

        assertEquals(result.getJSONArray("keys").getJSONObject(0).getString("kid"), "inline");
        verifyNoInteractions(uriService);
    }

    @Test
    public void getJwks_whenInlineJwksInvalidJson_shouldReturnNullWithoutUriFallback() {
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwks("not-json");
        config.setJwksUri(JWKS_URI);

        assertNull(service.getJwks(config, "kid"));
        verifyNoInteractions(uriService);
    }

    @Test
    public void getJwks_whenOnlyUri_shouldFetchFromUri() {
        when(uriService.loadJson(JWKS_URI)).thenReturn(new JSONObject(REMOTE_JWKS));
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);

        JSONObject result = service.getJwks(config, "remote");

        assertEquals(result.getJSONArray("keys").getJSONObject(0).getString("kid"), "remote");
    }

    @Test
    public void getJwks_whenUriNotHttps_shouldReturnNullWithoutFetching() {
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri("http://idp.example.com/jwks");

        assertNull(service.getJwks(config, "kid"));
        verifyNoInteractions(uriService);
    }

    @Test
    public void getJwks_whenCalledTwiceWithKnownKid_shouldFetchOnce() {
        when(uriService.loadJson(JWKS_URI)).thenReturn(new JSONObject(REMOTE_JWKS));
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);

        service.getJwks(config, "remote");
        service.getJwks(config, "remote");

        verify(uriService, times(1)).loadJson(JWKS_URI);
    }

    @Test
    public void getJwks_whenUnknownKidRightAfterFetch_shouldNotRefetch() {
        when(uriService.loadJson(JWKS_URI)).thenReturn(new JSONObject(REMOTE_JWKS));
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);

        service.getJwks(config, "remote");
        JSONObject result = service.getJwks(config, "rotated");

        assertNotNull(result);
        verify(uriService, times(1)).loadJson(JWKS_URI);
    }

    @Test
    public void getJwks_whenUriSchemeIsUpperCaseHttps_shouldFetch() {
        when(uriService.loadJson("HTTPS://idp.example.com/jwks")).thenReturn(new JSONObject(REMOTE_JWKS));
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri("HTTPS://idp.example.com/jwks");

        assertNotNull(service.getJwks(config, "remote"));
    }

    @Test
    public void getJwks_whenUriMalformed_shouldReturnNullWithoutFetching() {
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri("https://idp example.com/jwks");

        assertNull(service.getJwks(config, "kid"));
        verifyNoInteractions(uriService);
    }

    @Test
    public void getJwks_whenUnknownKidAfterRefetchInterval_shouldRefetch() {
        when(uriService.loadJson(JWKS_URI)).thenReturn(new JSONObject(REMOTE_JWKS));
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);
        doReturn(0L).when(service).now();
        service.getJwks(config, "remote");

        doReturn(31_000L).when(service).now();
        service.getJwks(config, "rotated");

        verify(uriService, times(2)).loadJson(JWKS_URI);
    }

    @Test
    public void getJwks_whenRefetchFailsWithinTtl_shouldKeepStaleKeysWithoutRenewingTtl() {
        when(uriService.loadJson(JWKS_URI)).thenReturn(new JSONObject(REMOTE_JWKS)).thenReturn(null);
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);
        doReturn(0L).when(service).now();
        service.getJwks(config, "remote");

        doReturn(31_000L).when(service).now();
        assertNotNull(service.getJwks(config, "rotated"));

        doReturn(301_000L).when(service).now();
        assertNull(service.getJwks(config, "remote"));
    }

    @Test
    public void getJwks_whenCachedKeysExpired_shouldRefetch() {
        when(uriService.loadJson(JWKS_URI)).thenReturn(new JSONObject(REMOTE_JWKS));
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);
        doReturn(0L).when(service).now();
        service.getJwks(config, "remote");

        doReturn(301_000L).when(service).now();
        service.getJwks(config, "remote");

        verify(uriService, times(2)).loadJson(JWKS_URI);
    }

    @Test
    public void getJwks_whenFetchFails_shouldReturnNullAndNegativeCache() {
        when(uriService.loadJson(JWKS_URI)).thenReturn(null);
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);

        assertNull(service.getJwks(config, "kid"));
        assertNull(service.getJwks(config, "kid"));

        verify(uriService, times(1)).loadJson(JWKS_URI);
    }

    @Test
    public void getJwks_whenResponseHasNoKeys_shouldReturnNull() {
        when(uriService.loadJson(JWKS_URI)).thenReturn(new JSONObject("{\"foo\":1}"));
        TrustedIssuerConfig config = new TrustedIssuerConfig();
        config.setJwksUri(JWKS_URI);

        assertNull(service.getJwks(config, "kid"));
    }
}
