package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;

import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

public class AuthorizationResponseAssertBuilder extends BaseAssertBuilder {

    private AuthorizationResponse response;
    private int status;
    private boolean nullState;
    private boolean nullScope;
    private List<ResponseType> responseTypes;

    public AuthorizationResponseAssertBuilder(AuthorizationResponse response) {
        this.response = response;
        this.status = 200;
        this.responseTypes = null;
        this.nullState = false;
        this.nullScope = false;
    }

    public AuthorizationResponseAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public AuthorizationResponseAssertBuilder nullState() {
        this.nullState = true;
        return this;
    }

    public AuthorizationResponseAssertBuilder nullScope() {
        this.nullState = true;
        return this;
    }

    public AuthorizationResponseAssertBuilder responseTypes(List<ResponseType> responseTypes) {
        this.responseTypes = responseTypes;
        return this;
    }

    @Override
    public void check() {
        assertNotNull(response, "AuthorizationResponse is null");
        if (status == 200) {
            assertNotNull(response.getLocation(), "The location is null");
            if (nullScope) {
                assertNull(response.getScope(), "The scope is not null");
            } else {
                assertNotNull(response.getScope(), "The scope is null");
            }
            if (nullState) {
                assertNull(response.getState(), "The state is not null");
            } else {
                assertNotNull(response.getState(), "The state is null");
            }

            if (this.responseTypes == null) {
                assertNotNull(response.getCode(), "The authorization code is null");
            } else {
                if (responseTypes.contains(ResponseType.CODE)) {
                    assertNotNull(response.getCode(), "The authorization code is null");
                }
                if (responseTypes.contains(ResponseType.TOKEN)) {
                    assertNotNull(response.getAccessToken(), "The access_token is null");
                    assertNotNull(response.getTokenType());
                    assertNotNull(response.getExpiresIn());
                }
                if (responseTypes.contains(ResponseType.ID_TOKEN)) {
                    assertNotNull(response.getIdToken(), "The id_token is null");
                }
            }
        } else {

        }
    }
}
