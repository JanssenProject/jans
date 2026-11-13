package io.jans.as.server.model.token;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.KeyOpsType;
import io.jans.as.model.jwk.Use;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.service.cdi.util.CdiUtil;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Listeners(MockitoTestNGListener.class)
public class JwtSignerTest {

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ConfigurationFactory configurationFactory;

    @Test
    public void newJwt_withExplicitSigningKid_shouldUseExplicitKid() throws Exception {
        JwtSigner signer = newSigner();
        signer.setSigningKid("explicit-kid");

        assertEquals(signer.newJwt().getHeader().getKeyId(), "explicit-kid");
        verify(cryptoProvider, never()).getKeyId(any(), any(), any(), any());
    }

    @Test
    public void newJwt_withExplicitSigningKidAndStaticKid_shouldPreferExplicitKid() throws Exception {
        lenient().when(appConfiguration.getStaticKid()).thenReturn("static-kid");
        JwtSigner signer = newSigner();
        signer.setSigningKid("explicit-kid");

        assertEquals(signer.newJwt().getHeader().getKeyId(), "explicit-kid");
    }

    @Test
    public void newJwt_withBlankSigningKidAndStaticKid_shouldUseStaticKid() throws Exception {
        when(appConfiguration.getStaticKid()).thenReturn("static-kid");
        JwtSigner signer = newSigner();
        signer.setSigningKid(" ");

        assertEquals(signer.newJwt().getHeader().getKeyId(), "static-kid");
    }

    @Test
    public void newJwt_withoutSigningKid_shouldLookupKeyByAlgorithm() throws Exception {
        when(cryptoProvider.getKeyId(any(JSONWebKeySet.class), eq(Algorithm.RS256), eq(Use.SIGNATURE), eq(KeyOpsType.CONNECT)))
                .thenReturn("lookup-kid");
        JwtSigner signer = newSigner();

        assertEquals(signer.newJwt().getHeader().getKeyId(), "lookup-kid");
    }

    @Test
    public void newJwtSigner_forClientWithIdTokenSigningKid_shouldNotSetSigningKid() {
        Client client = new Client();
        client.setClientId("client-id");
        client.getAttributes().setIdTokenSigningKid("id-token-kid");
        when(appConfiguration.getDefaultSignatureAlgorithm()).thenReturn("RS256");

        JwtSigner signer;
        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(AbstractCryptoProvider.class)).thenReturn(cryptoProvider);
            cdiUtil.when(() -> CdiUtil.bean(ConfigurationFactory.class)).thenReturn(configurationFactory);
            signer = JwtSigner.newJwtSigner(appConfiguration, new JSONWebKeySet(), client, "secret");
        }

        assertNull(signer.getSigningKid());
    }

    private JwtSigner newSigner() {
        return new JwtSigner(appConfiguration, new JSONWebKeySet(), SignatureAlgorithm.RS256, "audience", null, cryptoProvider);
    }
}
