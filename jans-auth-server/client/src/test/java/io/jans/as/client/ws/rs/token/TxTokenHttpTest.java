package io.jans.as.client.ws.rs.token;

import com.google.common.collect.Lists;
import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.*;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.Base64Util;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Transaction token test which tests two major functionality:
 * 1) obtain transaction token
 * 2) replace transaction token
 *
 * @author Yuriy Z
 */
public class TxTokenHttpTest extends BaseTest {

    private String txToken;

    @Parameters({"redirectUris"})
    @Test
    public void txTokenRequest(final String redirectUris) throws InvalidJwtException {
        showTitle("txTokenRequest");

        List<ResponseType> responseTypes = Lists.newArrayList(ResponseType.CODE, ResponseType.TOKEN);
        List<String> scopes = Lists.newArrayList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request subject_token
        TokenRequest subjectTokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        subjectTokenRequest.setAuthUsername(clientId);
        subjectTokenRequest.setAuthPassword(clientSecret);

        TokenClient subjectTokenClient = newTokenClient(subjectTokenRequest);
        TokenResponse subjectTokenResponse = subjectTokenClient.exec();

        showClient(subjectTokenClient);
        AssertBuilder.tokenResponse(subjectTokenResponse)
                .check();
        String subjectToken = subjectTokenResponse.getAccessToken();

        // 3. Request tx token using the subject token
        TokenRequest txTokenRequest = new TokenRequest(GrantType.TOKEN_EXCHANGE);
        txTokenRequest.setSubjectToken(subjectToken);
        txTokenRequest.setSubjectTokenType(SubjectTokenType.ACCESS_TOKEN.getName());
        txTokenRequest.setRequestedTokenType(ExchangeTokenType.TX_TOKEN.getName());
        txTokenRequest.setAudience("http://trusted.com");
        txTokenRequest.setRequestContext(Base64Util.base64urlencode("{\"req_ip\":\"69.151.72.123\"}"));

        TokenClient txTokenClient = newTokenClient(txTokenRequest);
        TokenResponse txTokenResponse = txTokenClient.exec();

        showClient(txTokenClient);
        assertEquals(txTokenResponse.getIssuedTokenType(), ExchangeTokenType.TX_TOKEN);

        txToken = txTokenResponse.getAccessToken();
        Jwt txTokenJwt = Jwt.parse(txToken);
        System.out.println("tx_token payload:");
        System.out.println(txTokenJwt.getClaims().toJsonString());

        // 4. introspect tx_token
        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint);
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken("Bearer " + subjectToken, txToken);

        assertNotNull(introspectionResponse);
        assertTrue(introspectionResponse.isActive());

        System.out.println("Introspection response for tx_token: " + txToken);
        System.out.println(introspectionResponse);
    }

    @Test(dependsOnMethods = {"txTokenRequest"})
    public void txTokenReplace() throws InvalidJwtException {
        showTitle("txTokenReplace");

        TokenRequest txTokenRequest = new TokenRequest(GrantType.TOKEN_EXCHANGE);
        txTokenRequest.setSubjectToken(txToken);
        txTokenRequest.setSubjectTokenType(SubjectTokenType.ACCESS_TOKEN.getName());
        txTokenRequest.setRequestedTokenType(ExchangeTokenType.TX_TOKEN.getName());
        txTokenRequest.setAudience("http://trusted2.com");
        txTokenRequest.setRequestContext("{\"req_ip\":\"69.151.72.100\"}");

        TokenClient txTokenClient = newTokenClient(txTokenRequest);
        TokenResponse txTokenResponse = txTokenClient.exec();

        showClient(txTokenClient);
        assertEquals(txTokenResponse.getIssuedTokenType(), ExchangeTokenType.TX_TOKEN);

        txToken = txTokenResponse.getAccessToken();
        Jwt txTokenJwt = Jwt.parse(txToken);
        System.out.println("tx_token payload:");
        System.out.println(txTokenJwt.getClaims().toJsonString());
    }

    public RegisterResponse registerClient(final String redirectUris, List<ResponseType> responseTypes, List<String> scopes) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "tx token test",
                io.jans.as.model.util.StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(List.of(GrantType.TOKEN_EXCHANGE, GrantType.CLIENT_CREDENTIALS));
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }
}
