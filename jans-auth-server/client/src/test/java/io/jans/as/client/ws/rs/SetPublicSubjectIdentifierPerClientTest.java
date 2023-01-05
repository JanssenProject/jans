/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static io.jans.as.model.common.ResponseType.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version February 11, 2022
 */
public class SetPublicSubjectIdentifierPerClientTest extends BaseTest {

    @Test(dataProvider = "dataMethod")
    public void setPublicSubjectIdentifierPerClient(
            final String redirectUri, final List<ResponseType> responseTypes, final String userId, final String userSecret,
            final String subjectIdentifierAttribute, final String expectedSubValue) throws InvalidJwtException {
        showTitle("setPublicSubjectIdentifierPerClient");

        RegisterResponse registerResponse = clientRegistration(redirectUri, responseTypes,
                SubjectType.PUBLIC, subjectIdentifierAttribute, false);

        String clientId = registerResponse.getClientId();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();

        Jwt jwt = Jwt.parse(idToken);
        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);
        assertTrue(rsaSigner.validate(jwt));

        assertEquals(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER), expectedSubValue);
    }

    @Parameters({"redirectUri"})
    @Test
    public void setPublicSubjectIdentifierPerClientFail1(final String redirectUri) {
        showTitle("setPublicSubjectIdentifierPerClientFail1");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        clientRegistration(redirectUri, responseTypes, SubjectType.PAIRWISE, "mail", true);
    }

    @Parameters({"redirectUri"})
    @Test
    public void setPublicSubjectIdentifierPerClientFail2(final String redirectUri) {
        showTitle("setPublicSubjectIdentifierPerClientFail2");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        clientRegistration(redirectUri, responseTypes, SubjectType.PUBLIC, "invalid_attribute", true);
    }

    @NotNull
    private RegisterResponse clientRegistration(
            String redirectUri, List<ResponseType> responseTypes, SubjectType subjectType,
            String subjectIdentifierAttribute, boolean checkError) {
        List<String> redirectUriList = Arrays.asList(redirectUri.split(StringUtils.SPACE));
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", redirectUriList);
        registerRequest.setSubjectType(subjectType);
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSubjectIdentifierAttribute(subjectIdentifierAttribute);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);

        if (checkError) {
            AssertBuilder.registerResponse(registerResponse).bad().check();
        } else {
            AssertBuilder.registerResponse(registerResponse).created().check();
        }

        return registerResponse;
    }

    @DataProvider(name = "dataMethod")
    public Object[] dataMethod(ITestContext context) {
        final String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");
        final String userId = context.getCurrentXmlTest().getParameter("userId");
        final String userSecret = context.getCurrentXmlTest().getParameter("userSecret");

        return new Object[][]{
                {redirectUri, Arrays.asList(TOKEN, ID_TOKEN), userId, userSecret, "mail", "test_user@test.org"},
                {redirectUri, Arrays.asList(TOKEN, ID_TOKEN), userId, userSecret, "uid", "test_user"},
                {redirectUri, Arrays.asList(CODE, TOKEN, ID_TOKEN), userId, userSecret, "mail", "test_user@test.org"},
                {redirectUri, Arrays.asList(CODE, TOKEN, ID_TOKEN), userId, userSecret, "uid", "test_user"}
        };
    }
}