/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common;

import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.util.Util;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.jans.as.model.authorize.AuthorizeRequestParam.*;
import static io.jans.as.model.authorize.AuthorizeResponseParam.RESPONSE;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Listeners(MockitoTestNGListener.class)
public class RedirectUriTest {

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Test
    public void parseQueryString_queryStringNull_EmptyResult() throws CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri.parseQueryString(null);
        assertEquals(redirectUri.getResponseParamentersSize(), 0);
    }

    @Test
    public void parseQueryString_oneParam_size1() throws CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri.parseQueryString(AuthorizeResponseParam.EXPIRES_IN + "=");
        assertEquals(redirectUri.getResponseParamentersSize(), 1);
    }

    @Test
    public void parseQueryString_threeParams_size3() throws CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri.parseQueryString(AuthorizeResponseParam.CODE + "=Pcode&" + AuthorizeResponseParam.EXPIRES_IN + "=3000&" + AuthorizeResponseParam.ACCESS_TOKEN + "=tk123");
        assertEquals(redirectUri.getResponseParamentersSize(), 3);
    }

    @Test
    public void parseQueryString_threeParamsOneNull_size3() throws CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri.parseQueryString(AuthorizeResponseParam.CODE + "=Pcode&" + AuthorizeResponseParam.EXPIRES_IN + "=&" + AuthorizeResponseParam.ACCESS_TOKEN + "=tk123");
        assertEquals(redirectUri.getResponseParamentersSize(), 3);
    }

    @Test
    public void getQueryString_noResponseMode_responseParametersEncoded() throws UnsupportedEncodingException, CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri.addResponseParameter(AuthorizeResponseParam.CODE, "123");
        redirectUri.addResponseParameter(AuthorizeResponseParam.EXPIRES_IN, "3000");
        redirectUri.addResponseParameter(AuthorizeResponseParam.ACCESS_TOKEN, "tk123");
        String queryResult = redirectUri.getQueryString();

        assertNoEmptyQueryString(queryResult, AuthorizeResponseParam.CODE, 3);
    }

    @Test()
    public void getQueryString_responseModeFormPostJWT_NoEmpty() throws CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.FORM_POST_JWT, null, null, null);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), Mockito.any())).thenReturn("12345");
        redirectUri.setCryptoProvider(cryptoProvider);
        String queryResult = redirectUri.getQueryString();
        //No empty Result
        assertTrue(queryResult.length() > 0);
    }

    @Test()
    public void getQueryString_withEncriptionAlgorithm128PublicKeyInvalid_responseEmptyThrowInvalid() throws CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256GCM, null);

        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.getPublicKey(anyString(), Mockito.any(), Mockito.any())).thenReturn(null);
        redirectUri.setCryptoProvider(cryptoProvider);

        String queryResult = redirectUri.getQueryString();
        assertEquals(queryResult, "");
    }

    @Test()
    public void getQueryString_withEncriptionAlgorithmRSANoSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException, CryptoProviderException, InvalidJweException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256GCM, null);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.getPublicKey(anyString(), Mockito.any(), Mockito.any())).thenReturn(getRSAPublicKey());
        Jwe jwe = mock(Jwe.class);
        when(jwe.encryptJwe(any(), any())).thenReturn(new Jwe());
        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.setJwtForEncrypt(jwe);
        redirectUri.addResponseParameter(AuthorizeResponseParam.EXPIRES_IN, "3000");

        String queryResult = redirectUri.getQueryString();

        assertNoEmptyQueryString(queryResult, RESPONSE, 1);
    }

    @Test()
    public void getQueryString_withEncriptionAlgorithm128NoSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException, CryptoProviderException, InvalidJweException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256GCM, null);
        Jwe jwe = mock(Jwe.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.getPublicKey(anyString(), Mockito.any(), Mockito.any())).thenReturn(getRSAPublicKey());
        when(jwe.encryptJwe(any(), any())).thenReturn(new Jwe());
        redirectUri.setJwtForEncrypt(jwe);
        redirectUri.setCryptoProvider(cryptoProvider);

        String queryResult = redirectUri.getQueryString();
        assertNoEmptyQueryString(queryResult, RESPONSE, 1);
    }

    @Test()
    public void getQueryString_withEncriptionAlgorithmRSAAndSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException, CryptoProviderException, InvalidJweException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256GCM, SignatureAlgorithm.HS256);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.getPublicKey(anyString(), Mockito.any(), Mockito.any())).thenReturn(getRSAPublicKey());
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), anyObject())).thenReturn("12345");

        Jwe jwe = mock(Jwe.class);
        when(jwe.encryptJwe(any(), any())).thenReturn(new Jwe());
        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.setJwtForEncrypt(jwe);
        redirectUri.setNestedKeyId("nestedKey123");
        redirectUri.setNestedSharedSecret("nested_shared_secret");

        String queryResult = redirectUri.getQueryString();

        assertNoEmptyQueryString(queryResult, RESPONSE, 1);
    }

    @Test()
    public void getQueryString_withEncriptionAlgorithm128KWAndSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException, CryptoProviderException, InvalidJweException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM, SignatureAlgorithm.HS256);

        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        Jwe jwe = mock(Jwe.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any(SignatureAlgorithm.class))).thenReturn("12345");
        when(jwe.encryptJwe(any(), any())).thenReturn(new Jwe());

        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.setJwtForEncrypt(jwe);
        redirectUri.setNestedKeyId("nestedKey123");
        redirectUri.setNestedSharedSecret("nested_shared_secret");

        String queryResult = redirectUri.getQueryString();

        assertNoEmptyQueryString(queryResult, RESPONSE, 1);

    }

    @Test()
    public void getQueryString_noEncriptionAlgorithmNoSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException, CryptoProviderException, InvalidJweException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, null, null, null);

        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any(SignatureAlgorithm.class))).thenReturn("12345");
        redirectUri.setCryptoProvider(cryptoProvider);

        String queryResult = redirectUri.getQueryString();

        assertNoEmptyQueryString(queryResult, RESPONSE, 1);

    }

    @Test()
    public void toString_withResponseModeFormPostJwtAndNoParams_NoEmpty() {

        RedirectUri redirectUri = getRedirectUriTemplateToString();
        redirectUri.setResponseMode(ResponseMode.FORM_POST_JWT);
        redirectUri.setCryptoProvider(cryptoProvider);

        assertTrue(redirectUri.toString().length() > 0, "Is Empty");
    }

    @Test()
    public void toString_withResponseModeFormPostAndNoParams_NoEmpty() {

        RedirectUri redirectUri = getRedirectUriTemplateToString();
        redirectUri.setResponseMode(ResponseMode.FORM_POST);
        assertTrue(redirectUri.toString().length() > 0, "is Empty");
    }

    @Test()
    public void toString_withResponseModeFragmentAndNoParams_NoEmpty() {
        RedirectUri redirectUri = getRedirectUriTemplateToString();
        redirectUri.setResponseMode(ResponseMode.FRAGMENT);
        redirectUri.setCryptoProvider(cryptoProvider);

        assertTrue(redirectUri.toString().length() > 0, "is Empty");
    }

    @Test()
    public void toString_withResponseModeQueryAndNoParams_NoEmpty() {
        RedirectUri redirectUri = getRedirectUriTemplateToString();
        redirectUri.setResponseMode(ResponseMode.FRAGMENT);
        redirectUri.setCryptoProvider(cryptoProvider);

        assertTrue(redirectUri.toString().length() > 0, "is Empty");
    }

    @Test()
    public void toString_withResponseModeJwtAndNoParams_NoEmpty() {
        String baseUri = "http://redirecturl.com/";
        List<ResponseType> typeList = new ArrayList<>();
        typeList.add(ResponseType.TOKEN);
        RedirectUri redirectUri = new RedirectUri(baseUri, typeList, null);
        redirectUri.addResponseParameter(CLIENT_ID, "123");
        redirectUri.setResponseMode(ResponseMode.JWT);
        redirectUri.setCryptoProvider(cryptoProvider);

        assertTrue(redirectUri.toString().length() > 0, "is Empty");
    }

    @Test()
    public void toString_withResponseModeNullAndResponseParams_NoEmpty() {
        RedirectUri redirectUri = getRedirectUriTemplateToString();
        assertTrue(redirectUri.toString().length() > 0, "is Empty");
    }

    @Test()
    public void toString_withResponseModeNullWithTokenResponseType_NoEmpty() {
        String baseUri = "http://redirecturl.com/";
        List<ResponseType> typeList = new ArrayList<>();
        typeList.add(ResponseType.TOKEN);
        RedirectUri redirectUri = new RedirectUri(baseUri, typeList, null);
        redirectUri.addResponseParameter(CLIENT_ID, "123");
        assertTrue(redirectUri.toString().length() > 0, "is Empty");
    }

    @Test()
    public void toString_NoResponseParams_sameBaseUri() {
        String baseUri = "http://redirecturl.com/";
        RedirectUri redirectUri = new RedirectUri(baseUri);
        assertEquals(redirectUri.toString(), baseUri);
    }

    private RedirectUri getRedirectUriTemplateToString() {
        String clientId = "123";
        String responseType = "token id_token";
        String scope = "openid";
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        RedirectUri redirectUri = new RedirectUri("http://redirecturl.com/");
        redirectUri.addResponseParameter(CLIENT_ID, clientId);
        redirectUri.addResponseParameter(REDIRECT_URI, redirectUri.getBaseRedirectUri());
        redirectUri.addResponseParameter(RESPONSE_TYPE, responseType);
        redirectUri.addResponseParameter(SCOPE, scope);
        redirectUri.addResponseParameter(STATE, state);
        redirectUri.addResponseParameter(NONCE, nonce);
        redirectUri.setAuthorizationCodeLifetime(1000);
        redirectUri.setBlockEncryptionAlgorithm(null);
        redirectUri.setNestedKeyId("nki");
        redirectUri.setResponseMode(null);
        return redirectUri;
    }

    private RedirectUri getRedirectUriTemplateGetQueryString(ResponseMode responseMode, KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, SignatureAlgorithm signatureAlgorithm) throws CryptoProviderException {
        RedirectUri redirectUri = new RedirectUri("http://redirecturl.com/", new ArrayList<>(), responseMode);
        redirectUri.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
        redirectUri.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);
        redirectUri.setSignatureAlgorithm(signatureAlgorithm);
        redirectUri.setIssuer("user123");
        redirectUri.setKeyId("key123");
        redirectUri.setSharedSecret("shared_secret");
        redirectUri.setJsonWebKeys(null);
        redirectUri.setCryptoProvider(cryptoProvider);
        return redirectUri;
    }

    private RSAPublicKey getRSAPublicKey() {
        return new RSAPublicKey() {
            @Override
            public BigInteger getPublicExponent() {
                return null;
            }

            @Override
            public String getAlgorithm() {
                return null;
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public BigInteger getModulus() {
                return null;
            }
        };
    }

    private void assertNoEmptyQueryString(String queryResult, String paramToVerify, int paramsSize) throws UnsupportedEncodingException, CryptoProviderException {
        //No empty result
        assertTrue(queryResult.length() > 0);

        //the queryResult contains "paramToVerify" key
        assertTrue(queryResult.contains(URLEncoder.encode(paramToVerify, Util.UTF8_STRING_ENCODING)));

        //the size params is verified parsing
        RedirectUri redirectUri2 = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri2.parseQueryString(queryResult);
        assertEquals(redirectUri2.getResponseParamentersSize(), paramsSize);
    }

}
