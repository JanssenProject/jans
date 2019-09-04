package org.gluu.oxd.mock.service;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.TokenType;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.server.op.OpClientFactory;
import org.gluu.oxd.server.op.Validator;
import org.gluu.oxd.server.service.PublicOpKeyService;

import java.util.Calendar;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpClientFactoryMockImpl implements OpClientFactory {
    @Override
    public TokenClient createTokenClient(String url) {
        TokenClient client = mock(TokenClient.class);

        TokenResponse response = new TokenResponse();
        response.setAccessToken("DUMMY_ACCESS_TOKEN_"+ System.currentTimeMillis());
        response.setTokenType(TokenType.BEARER);
        response.setExpiresIn(50000);
        response.setRefreshToken(null);
        response.setScope("openid");
        response.setIdToken("eyJraWQiOiJjZmFiMzRlYy0xNjhkLTQ4OTUtODRiOC0xZjAyNzgwNDkxYzciLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiaVFPZDJ2aEtWVWFzRVRCRDZEbjV0ZyIsImF1ZCI6IjIwNDE5ZDRkLTRhMGItNGIyOC05MjgwLTkzNmNlZDBkNjVmZSIsInN1YiI6InMtX1ppclZ0N05PRGRuV0RBVUdyalQycVVad0s2Y1hUaGI5cVY5OXYtdGciLCJhdXRoX3RpbWUiOjE1Njc1MDY4MTUsImlzcyI6Imh0dHBzOi8vY2UtZGV2Ni5nbHV1Lm9yZyIsImV4cCI6MTU2NzUxMDQxOCwiaWF0IjoxNTY3NTA2ODE4LCJub25jZSI6IjVqOTlyMW9tb2Q1azQ3MTFmMnB1ZDMzZTBhIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.Wt_pUXW1BJyjcq2WJUMIYZwzEUeAmrDe8SaWM-RC7T86TmnQOnz0JMgEEN1J9ONsJNMdf8WJZDaqWXu2tVqHh1IrWmZ-U8_36HxcgPXy65yLho0hzCdjPp_KVTdttQhOmLvqn9x_NO8p06wBjm3d5T6xOgtxOjR0c4lqMOBDh3_jb9UH5ZLHRosx9pFCluylPjok8BREmOI_YnUJKHWz2Js9juWBnE94s50EOb7JuyVHvIDvVkrfh0YRZw61idaRQYzfEzwQQYJz6MF2xd4eHT3f-iB5ZBYdrtOPk0691ogLL3HbO_pCfjvsf4QVD0Q-4rlcSJ004ORyR77cgrBSAA");
        response.setStatus(200);

        when(client.exec()).thenReturn(response);
        when(client.execClientCredentialsGrant(any(), any(), any())).thenReturn(response);

        return client;
    }

    @Override
    public UserInfoClient createUserInfoClient(String url) {
        UserInfoClient client = mock(UserInfoClient.class);

        UserInfoResponse response = new UserInfoResponse(0);
        response.setEntity("{ \"name\":\"John\", \"age\":30, \"sub\":\"present\" }");

        when(client.exec()).thenReturn(response);
        return client;
    }

    @Override
    public RegisterClient createRegisterClient(String url) {
        RegisterClient client = mock(RegisterClient.class);

        RegisterResponse response = new RegisterResponse();

        response.setClientId("67c0f792-4f03-4146-b31b-575c8da21ca6");
        response.setClientSecret("DUMMY_CLIENT_SECRET_"+ System.currentTimeMillis());
        response.setRegistrationAccessToken("DUMMY_REGISTRATION_ACCESS_TOKEN");
        response.setRegistrationClientUri("https://www.dummy-op-server.xyz/oxauth/restv1/register?client_id=@!8DBF.24EB.FA0E.1BFF!0001!32B7.932A!0008!AB90.6BF3.8E32.7A13");

        Calendar calendar = Calendar.getInstance();
        // get a date to represent "today"
        Date today = calendar.getTime();
        // add one day to the date/calendar
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = calendar.getTime();

        response.setClientIdIssuedAt(today);
        response.setClientSecretExpiresAt(tomorrow);
        when(client.exec()).thenReturn(response);
        return client;
    }

    @Override
    public OpenIdConfigurationClient createOpenIdConfigurationClient(String url) {
        return null;
    }

    @Override
    public AuthorizeClient createAuthorizeClient(String url) {

        AuthorizeClient client = mock(AuthorizeClient.class);

        AuthorizationResponse response = new AuthorizationResponse("");
        response.setCode("DUMMY_CODE_"+ System.currentTimeMillis());
        response.setScope("DUMMY_SCOPE_"+ System.currentTimeMillis());

        when(client.exec()).thenReturn(response);

        return client;
    }

    public Validator createValidator(Jwt idToken, OpenIdConfigurationResponse discoveryResponse, PublicOpKeyService keyService) {
        Validator client = mock(Validator.class);
        //when(client.validateIdToken(any())).thenReturn(null);
        return client;
    }
}
