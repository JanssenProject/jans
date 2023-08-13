/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.util;

import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.util.Util;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;

import static io.jans.as.model.authorize.AuthorizeRequestParam.*;
import static io.jans.as.model.authorize.AuthorizeResponseParam.EXPIRES_IN;
import static io.jans.as.model.authorize.AuthorizeResponseParam.RESPONSE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class RedirectUriTest {

    @Test
    public void parseQueryString_queryStringNull_EmptyResult() {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri.parseQueryString(null);
        assertEquals(redirectUri.getResponseParamentersSize(), 0);
    }

    @Test
    public void parseQueryString_oneParam_size1() throws UnsupportedEncodingException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri.parseQueryString(encodeUTF8(AuthorizeResponseParam.EXPIRES_IN) + "=");
        assertEquals(redirectUri.getResponseParamentersSize(), 1);
        assertNull(redirectUri.getResponseParameter(AuthorizeResponseParam.EXPIRES_IN));
    }

    @Test
    public void parseQueryString_threeParams_size3() throws UnsupportedEncodingException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        String p_code = encodeUTF8(AuthorizeResponseParam.CODE);
        String p_expires_in = encodeUTF8(AuthorizeResponseParam.EXPIRES_IN);
        String p_access_token = encodeUTF8(AuthorizeResponseParam.ACCESS_TOKEN);
        String code = encodeUTF8("12345");
        String expires_in = encodeUTF8("30000");
        String token = encodeUTF8("tk123");
        redirectUri.parseQueryString(p_code + "=" + code + "&" + p_expires_in + "=" + expires_in + "&" + p_access_token + "=" + token);
        assertEquals(redirectUri.getResponseParamentersSize(), 3);
        assertEquals(redirectUri.getResponseParameter(p_code), code);
        assertEquals(redirectUri.getResponseParameter(p_expires_in), expires_in);
        assertEquals(redirectUri.getResponseParameter(p_access_token), token);
    }

    @Test
    public void parseQueryString_threeParamsOneNull_size3() throws UnsupportedEncodingException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        String p_code = encodeUTF8(AuthorizeResponseParam.CODE);
        String p_expires_in = encodeUTF8(AuthorizeResponseParam.EXPIRES_IN);
        String p_token = encodeUTF8(AuthorizeResponseParam.ACCESS_TOKEN);
        String code = encodeUTF8("12345");
        String token = encodeUTF8("tk123");
        redirectUri.parseQueryString(p_code + "=" + code + "&" + p_expires_in + "=&" + p_token + "=" + token);
        assertEquals(redirectUri.getResponseParamentersSize(), 3);
        assertEquals(redirectUri.getResponseParameter(p_code), code);
        assertNull(redirectUri.getResponseParameter(p_expires_in));
        assertEquals(redirectUri.getResponseParameter(p_token), token);
    }

    @Test
    public void getQueryString_noResponseMode_responseParametersEncoded() throws UnsupportedEncodingException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri.addResponseParameter(AuthorizeResponseParam.CODE, "12345");
        redirectUri.addResponseParameter(AuthorizeResponseParam.EXPIRES_IN, "30000");
        redirectUri.addResponseParameter(AuthorizeResponseParam.ACCESS_TOKEN, "tk12345");
        String queryResult = redirectUri.getQueryString();
        System.out.println(queryResult);
        assertNoEmptyQueryString(queryResult, AuthorizeResponseParam.CODE, 3);
        assertEquals(queryResult, "access_token=tk12345&code=12345&expires_in=30000");
    }

    @Test
    public void getQueryString_responseModeFormPostJWT_responseEncodedNoEmpty() throws CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.FORM_POST_JWT, null, null, null);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any())).thenReturn("12345");
        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.addResponseParameter(AuthorizeResponseParam.EXPIRES_IN, "3000");
        String queryResult = redirectUri.getQueryString();
        System.out.println(queryResult);
        //No empty Result
        assertTrue(queryResult.length() > 0);
        assertEquals(queryResult, "eyJraWQiOiJrZXkxMjMiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1c2VyMTIzIiwiZXhwIjoiMzAwMCIsImV4cGlyZXNfaW4iOiIzMDAwIn0.12345");
    }

    @Test
    public void getQueryString_withEncriptionAlgorithm128PublicKeyInvalid_responseEmptyThrowInvalid() throws CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256GCM, null);

        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.getPublicKey(anyString(), any(), any())).thenReturn(null);
        redirectUri.setCryptoProvider(cryptoProvider);

        String queryResult = redirectUri.getQueryString();
        System.out.println(queryResult);
        assertEquals(queryResult, "");
    }

    @Test
    public void getQueryString_withEncriptionAlgorithmRSANoSignatureAlgorithm_responseEncoded() throws CryptoProviderException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256GCM, null);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.getPublicKey(anyString(), any(), any())).thenReturn(getRSAPublicKey());

        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.addResponseParameter(AuthorizeResponseParam.EXPIRES_IN, "1644270473301");

        String queryResult = redirectUri.getQueryString();
        System.out.println(queryResult);
        assertNoEmptyQueryString(queryResult, RESPONSE, 1);
        assertTrue(queryResult.startsWith("response=eyJ0eXAiOiJqd3QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBMV81In0."));
    }

    @Test
    public void getQueryString_withEncriptionAlgorithm128NoSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM, null);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);

        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.setNestedKeyId("nestedKey123");
        redirectUri.setNestedSharedSecret("nested_shared_secret");
        redirectUri.setSharedSymmetricKey("0123456789012345".getBytes());
        redirectUri.addResponseParameter(EXPIRES_IN, "1644270473301");

        String queryResult = redirectUri.getQueryString();
        System.out.println(queryResult);
        assertNoEmptyQueryString(queryResult, RESPONSE, 1);
        assertTrue(queryResult.startsWith("response=eyJ0eXAiOiJqd3QiLCJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiQTEyOEtXIn0."));
    }

    @Test
    public void getQueryString_withEncriptionAlgorithmRSAAndSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException, CryptoProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256GCM, SignatureAlgorithm.HS256);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.getPublicKey(anyString(), any(), any())).thenReturn(getRSAPublicKey());
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any())).thenReturn("12345");

        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.setNestedKeyId("nestedKey123");
        redirectUri.setNestedSharedSecret("nested_shared_secret");

        String queryResult = redirectUri.getQueryString();
        System.out.println(queryResult);
        assertNoEmptyQueryString(queryResult, RESPONSE, 1);
        assertTrue(queryResult.startsWith("response=eyJjdHkiOiJqd3QiLCJ0eXAiOiJqd3QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBMV81In0."));
    }

    @Test
    public void getQueryString_withEncriptionAlgorithm128KWAndSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException, CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM, SignatureAlgorithm.HS256);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any(SignatureAlgorithm.class))).thenReturn("12345");

        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.setNestedKeyId("nestedKey123");
        redirectUri.setNestedSharedSecret("nested_shared_secret");
        redirectUri.setSharedSymmetricKey("0123456789012345".getBytes());
        redirectUri.addResponseParameter(EXPIRES_IN, "1644270473301");

        String queryResult = redirectUri.getQueryString();
        System.out.println(queryResult);

        assertNoEmptyQueryString(queryResult, RESPONSE, 1);
        assertTrue(queryResult.startsWith("response=eyJjdHkiOiJqd3QiLCJ0eXAiOiJqd3QiLCJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiQTEyOEtXIn0."));
    }

    @Test
    public void getQueryString_noEncriptionAlgorithmNoSignatureAlgorithm_responseEncoded() throws UnsupportedEncodingException, CryptoProviderException {
        RedirectUri redirectUri = getRedirectUriTemplateGetQueryString(ResponseMode.JWT, null, null, null);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any(SignatureAlgorithm.class))).thenReturn("12345");
        redirectUri.setCryptoProvider(cryptoProvider);

        String queryResult = redirectUri.getQueryString();
        System.out.println(queryResult);

        assertNoEmptyQueryString(queryResult, RESPONSE, 1);
        assertTrue(queryResult.startsWith("response=eyJraWQiOiJrZXkxMjMiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9."));
    }

    @Test
    public void toString_withResponseModeFormPostJwt_validHtmlFormResponse() throws CryptoProviderException {
        String valTestCase = "<html><head><title>Submit This Form</title></head><body onload=\"javascript:document.forms[0].submit()\"><form method=\"post\" action=\"http://redirecturl.com/\"><input type=\"hidden\" name=\"response\" value=\"eyJraWQiOiJrZXkxMjMiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJzY29wZSI6Im9wZW5pZCIsInJlc3BvbnNlX3R5cGUiOiJ0b2tlbiBpZF90b2tlbiIsInJlZGlyZWN0X3VyaSI6Imh0dHA6Ly9yZWRpcmVjdHVybC5jb20vIiwic3RhdGUiOiJjOTU1NzBkMS01YWI4LTQ2OGItOWMwMS05Y2M2MGUwMmIwMjMiLCJleHAiOiIxNjQ0MjcwNDczMzAxIiwibm9uY2UiOiIwZGEwZDA0Yi1hNmJkLTRkOWUtOGJkOS0yMTE2NWYwZDNiYjciLCJleHBpcmVzX2luIjoiMTY0NDI3MDQ3MzMwMSIsImNsaWVudF9pZCI6IjEyMyJ9.12345\"/></form></body></html>";
        RedirectUri redirectUri = getRedirectUriTemplateToString();
        redirectUri.setResponseMode(ResponseMode.FORM_POST_JWT);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any(SignatureAlgorithm.class))).thenReturn("12345");
        redirectUri.setCryptoProvider(cryptoProvider);
        redirectUri.setKeyId("key123");
        redirectUri.setSharedSecret("shared_secret");
        redirectUri.setNestedSharedSecret("nested_shared_secret");

        assertEquals(redirectUri.toString(), valTestCase);
    }

    @Test
    public void toString_withResponseModeFormPost_validHtmlFormResponse() {
        String valTestCase = "<html><head><title>Submit This Form</title></head><body onload=\"javascript:document.forms[0].submit()\"><form method=\"post\" action=\"http://redirecturl.com/\"><input type=\"hidden\" name=\"scope\" value=\"openid\"/><input type=\"hidden\" name=\"response_type\" value=\"token id_token\"/><input type=\"hidden\" name=\"redirect_uri\" value=\"http://redirecturl.com/\"/><input type=\"hidden\" name=\"state\" value=\"c95570d1-5ab8-468b-9c01-9cc60e02b023\"/><input type=\"hidden\" name=\"nonce\" value=\"0da0d04b-a6bd-4d9e-8bd9-21165f0d3bb7\"/><input type=\"hidden\" name=\"expires_in\" value=\"1644270473301\"/><input type=\"hidden\" name=\"client_id\" value=\"123\"/></form></body></html>";
        RedirectUri redirectUri = getRedirectUriTemplateToString();
        redirectUri.setResponseMode(ResponseMode.FORM_POST);

        assertEquals(redirectUri.toString(), valTestCase);
    }

    @Test
    public void toString_withResponseModeFragment_validURLFragmentString() {
        String valTestCase = "http://redirecturl.com/#scope=openid&response_type=token+id_token&redirect_uri=http%3A%2F%2Fredirecturl.com%2F&state=c95570d1-5ab8-468b-9c01-9cc60e02b023&nonce=0da0d04b-a6bd-4d9e-8bd9-21165f0d3bb7&expires_in=1644270473301&client_id=123";
        RedirectUri redirectUri = getRedirectUriTemplateToString();
        redirectUri.setResponseMode(ResponseMode.FRAGMENT);

        assertEquals(redirectUri.toString(), valTestCase);
    }

    @Test
    public void toString_withResponseModeQuery_validURLQueryString() {
        String valTestCase = "http://redirecturl.com/?scope=openid&response_type=token+id_token&redirect_uri=http%3A%2F%2Fredirecturl.com%2F&state=c95570d1-5ab8-468b-9c01-9cc60e02b023&nonce=0da0d04b-a6bd-4d9e-8bd9-21165f0d3bb7&expires_in=1644270473301&client_id=123";
        RedirectUri redirectUri = getRedirectUriTemplateToString();
        redirectUri.setResponseMode(ResponseMode.QUERY);

        assertEquals(redirectUri.toString(), valTestCase);
    }

    @Test
    public void toString_withResponseModeJwtAndresponseTypeToken_validURLFragmentString() throws CryptoProviderException {
        String valTestCase = "http://redirecturl.com/#response=eyJraWQiOiJrZXkxMjMiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOiIxNjQ0MjcwNDczMzAxIiwiZXhwaXJlc19pbiI6IjE2NDQyNzA0NzMzMDEiLCJjbGllbnRfaWQiOiIxMjMifQ.12345";
        List<ResponseType> typeList = new ArrayList<>();
        typeList.add(ResponseType.TOKEN);
        RedirectUri redirectUri = new RedirectUri("http://redirecturl.com/", typeList, ResponseMode.JWT);
        redirectUri.addResponseParameter(CLIENT_ID, "123");
        redirectUri.addResponseParameter(EXPIRES_IN, "1644270473301");
        redirectUri.setKeyId("key123");
        redirectUri.setSharedSecret("shared_secret");
        redirectUri.setNestedSharedSecret("nested_shared_secret");

        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any(SignatureAlgorithm.class))).thenReturn("12345");
        redirectUri.setCryptoProvider(cryptoProvider);

        assertEquals(redirectUri.toString(), valTestCase);
    }

    @Test
    public void toString_withResponseModeJwtAndresponseTypeCode_validURLQueryString() throws CryptoProviderException {
        String valTestCase = "http://redirecturl.com/?response=eyJraWQiOiJrZXkxMjMiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJleHAiOiIxNjQ0MjcwNDczMzAxIiwiZXhwaXJlc19pbiI6IjE2NDQyNzA0NzMzMDEiLCJjbGllbnRfaWQiOiIxMjMifQ.12345";
        List<ResponseType> typeList = new ArrayList<>();
        typeList.add(ResponseType.CODE);
        RedirectUri redirectUri = new RedirectUri("http://redirecturl.com/", typeList, ResponseMode.JWT);
        redirectUri.setKeyId("key123");
        redirectUri.setSharedSecret("shared_secret");
        redirectUri.addResponseParameter(CLIENT_ID, "123");
        redirectUri.addResponseParameter(EXPIRES_IN, "1644270473301");

        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(cryptoProvider.sign(anyString(), anyString(), anyString(), any(SignatureAlgorithm.class))).thenReturn("12345");
        redirectUri.setCryptoProvider(cryptoProvider);

        assertEquals(redirectUri.toString(), valTestCase);
    }

    @Test
    public void toString_nullResponseModeResponseTypeToken_validURLFragmentString() {
        String valTestCase = "http://redirecturl.com/#expires_in=1644270473301&client_id=123";
        List<ResponseType> typeList = new ArrayList<>();
        typeList.add(ResponseType.TOKEN);
        RedirectUri redirectUri = new RedirectUri("http://redirecturl.com/", typeList, null);
        redirectUri.addResponseParameter(CLIENT_ID, "123");
        redirectUri.addResponseParameter(EXPIRES_IN, "1644270473301");

        assertEquals(redirectUri.toString(), valTestCase);
    }

    @Test
    public void toString_nullResponseModeNoResponseType_validURLQueryString() {
        String valTestCase = "http://redirecturl.com/?expires_in=1644270473301&client_id=123";
        RedirectUri redirectUri = new RedirectUri("http://redirecturl.com/", null, null);
        redirectUri.addResponseParameter(CLIENT_ID, "123");
        redirectUri.addResponseParameter(EXPIRES_IN, "1644270473301");

        assertEquals(redirectUri.toString(), valTestCase);
    }

    @Test
    public void toString_NoResponseParams_sameBaseUri() {
        String baseUri = "http://redirecturl.com/";
        RedirectUri redirectUri = new RedirectUri(baseUri);

        assertEquals(redirectUri.toString(), baseUri);
    }

    private RedirectUri getRedirectUriTemplateToString() {
        String clientId = "123";
        String responseType = "token id_token";
        String scope = "openid";
        String state = "c95570d1-5ab8-468b-9c01-9cc60e02b023";
        String nonce = "0da0d04b-a6bd-4d9e-8bd9-21165f0d3bb7";

        RedirectUri redirectUri = spy(new RedirectUri("http://redirecturl.com/"));
        redirectUri.addResponseParameter(CLIENT_ID, clientId);
        redirectUri.addResponseParameter(REDIRECT_URI, redirectUri.getBaseRedirectUri());
        redirectUri.addResponseParameter(RESPONSE_TYPE, responseType);
        redirectUri.addResponseParameter(SCOPE, scope);
        redirectUri.addResponseParameter(STATE, state);
        redirectUri.addResponseParameter(NONCE, nonce);
        redirectUri.addResponseParameter(EXPIRES_IN, "1644270473301");
        redirectUri.setAuthorizationCodeLifetime(1000);
        redirectUri.setBlockEncryptionAlgorithm(null);
        redirectUri.setNestedKeyId("nki");
        redirectUri.setResponseMode(null);
        return redirectUri;
    }

    private RedirectUri getRedirectUriTemplateGetQueryString(ResponseMode responseMode, KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, SignatureAlgorithm signatureAlgorithm) {
        RedirectUri redirectUri = spy(new RedirectUri("http://redirecturl.com/", new ArrayList<>(), responseMode));
        redirectUri.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
        redirectUri.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);
        redirectUri.setSignatureAlgorithm(signatureAlgorithm);
        redirectUri.setIssuer("user123");
        redirectUri.setKeyId("key123");
        redirectUri.setSharedSecret("shared_secret");
        redirectUri.setJsonWebKeys(null);
        return redirectUri;
    }

    private void assertNoEmptyQueryString(String queryResult, String paramToVerify, int paramsSize) throws UnsupportedEncodingException {
        //No empty result
        assertTrue(queryResult.length() > 0);

        //the queryResult contains "paramToVerify" key
        assertTrue(queryResult.contains(URLEncoder.encode(paramToVerify, Util.UTF8_STRING_ENCODING)));

        //the size params is verified parsing
        RedirectUri redirectUri2 = getRedirectUriTemplateGetQueryString(null, null, null, null);
        redirectUri2.parseQueryString(queryResult);
        assertEquals(redirectUri2.getResponseParamentersSize(), paramsSize);
    }

    private String encodeUTF8(String input) throws UnsupportedEncodingException {
        return URLEncoder.encode(input, Util.UTF8_STRING_ENCODING);
    }

    private RSAPublicKey getRSAPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
        String publicExponent = "010001";
        RSAPublicKeySpec rsaPubKS = new RSAPublicKeySpec(new BigInteger(modulus, 16), new BigInteger(publicExponent, 16));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(rsaPubKS);
    }

}
