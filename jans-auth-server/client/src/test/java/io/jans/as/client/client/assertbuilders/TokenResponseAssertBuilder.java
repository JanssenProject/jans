package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.TokenResponse;
import io.jans.as.model.token.TokenErrorResponseType;

import static io.jans.as.client.client.Asserter.assertNotBlank;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TokenResponseAssertBuilder extends BaseAssertBuilder {

    private TokenResponse response;
    private int status = 200;
    private boolean notNullRefreshToken;
    private boolean notBlankDeviceToken;
    private boolean notNullIdToken;
    private boolean notNullScope;
    private boolean nullRefreshToken;
    private TokenErrorResponseType errorResponseType;

    public TokenResponseAssertBuilder(TokenResponse response) {
        this.response = response;
        this.status = 200;
        this.notNullIdToken = false;
        this.notNullRefreshToken = false;
        this.notNullScope = false;
        this.nullRefreshToken = false;
    }

    public TokenResponseAssertBuilder ok() {
        this.status = 200;
        return this;
    }

    public TokenResponseAssertBuilder created() {
        this.status = 201;
        return this;
    }

    public TokenResponseAssertBuilder bad(TokenErrorResponseType responseType) {
        this.status = 400;
        this.errorResponseType = responseType;
        return this;
    }

    public TokenResponseAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public TokenResponseAssertBuilder notNullRefreshToken() {
        this.notNullRefreshToken = true;
        this.nullRefreshToken = false;
        return this;
    }

    public TokenResponseAssertBuilder notBlankDeviceToken() {
        this.notBlankDeviceToken = true;
        return this;
    }

    public TokenResponseAssertBuilder notNullIdToken() {
        this.notNullIdToken = true;
        return this;
    }

    public TokenResponseAssertBuilder notNullScope() {
        this.notNullScope = true;
        return this;
    }

    public TokenResponseAssertBuilder nullRefreshToken() {
        this.nullRefreshToken = true;
        this.notNullRefreshToken = false;
        return this;
    }

    public TokenResponseAssertBuilder errorResponseType(TokenErrorResponseType errorResponseType) {
        this.errorResponseType = errorResponseType;
        return this;
    }

    @Override
    public void check() {
        assertNotNull(response, "TokenResponse is null");
        if (status == 200 || status == 201) {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getStatus());
            assertNotNull(response.getEntity(), "The entity is null");
            assertNotNull(response.getAccessToken(), "The access token is null");
            assertNotNull(response.getExpiresIn(), "The expires in value is null");
            assertNotNull(response.getTokenType(), "The token type is null");
            if (notNullIdToken) {
                assertNotNull(response.getIdToken(), "The id token is null");
            }
            if (notNullRefreshToken) {
                assertNotNull(response.getRefreshToken(), "The refresh token is null");
            }
            if (notBlankDeviceToken) {
                assertNotBlank(response.getDeviceToken(), "The device token is blank");
            }
        } else {
            assertEquals(response.getStatus(), status, "Unexpected HTTP status response: " + response.getEntity());
            assertNotNull(response.getEntity(), "The entity is null");
            if (errorResponseType != null) {
                assertEquals(response.getErrorType(), errorResponseType, "Unexpected error type, should be " + errorResponseType.getParameter());
            }
            assertNotNull(response.getErrorDescription());
        }
    }
}
