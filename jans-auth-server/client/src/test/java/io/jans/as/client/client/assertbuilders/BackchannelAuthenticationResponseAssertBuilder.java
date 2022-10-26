package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.BackchannelAuthenticationResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;

import static org.testng.Assert.*;

public class BackchannelAuthenticationResponseAssertBuilder extends BaseAssertBuilder {

    private BackchannelAuthenticationResponse response;
    private int status;
    private boolean notNullAuthReqId;
    private boolean notNullExpiresIn;
    private boolean nullAuthReqId;
    private boolean nullExpiresIn;
    private boolean notNullInterval;
    private boolean nullInterval;
    private BackchannelAuthenticationErrorResponseType errorResponseType;

    public BackchannelAuthenticationResponseAssertBuilder(BackchannelAuthenticationResponse response) {
        this.response = response;
        this.status = 200;
        this.notNullAuthReqId = false;
        this.notNullExpiresIn = false;
        this.nullAuthReqId = false;
        this.nullExpiresIn = false;
        this.notNullInterval = false;
        this.nullInterval = false;
        this.errorResponseType = null;
    }

    public BackchannelAuthenticationResponseAssertBuilder ok() {
        this.status = 200;
        this.notNullAuthReqId = true;
        this.notNullExpiresIn = true;
        this.notNullInterval = true;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder bad(BackchannelAuthenticationErrorResponseType responseType) {
        this.status = 400;
        this.errorResponseType = responseType;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder unauthorized(BackchannelAuthenticationErrorResponseType responseType) {
        this.status = 401;
        this.errorResponseType = responseType;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder nullAuthReqId() {
        this.notNullAuthReqId = false;
        this.nullAuthReqId = true;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder nullExpiresIn() {
        this.notNullExpiresIn = false;
        this.nullExpiresIn = true;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder notNullAuthReqId() {
        this.notNullAuthReqId = true;
        this.nullAuthReqId = false;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder notNullExpiresIn() {
        this.notNullExpiresIn = true;
        this.nullExpiresIn = false;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder notNullInterval() {
        this.notNullInterval = true;
        this.nullInterval = false;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder nullInterval() {
        this.notNullInterval = false;
        this.nullInterval = true;
        return this;
    }

    public BackchannelAuthenticationResponseAssertBuilder errorResponseType(BackchannelAuthenticationErrorResponseType errorResponseType) {
        this.errorResponseType = errorResponseType;
        return this;
    }

    @Override
    public void check() {
        assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
        if (notNullInterval) {
            assertNotNull(response.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.
        } else if (nullInterval) {
            assertNull(response.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.
        }
        if (notNullAuthReqId) {
            assertNotNull(response.getAuthReqId());
        } else if (nullAuthReqId) {
            assertNull(response.getAuthReqId());
        }
        if (notNullExpiresIn) {
            assertNotNull(response.getExpiresIn());
        } else if (nullExpiresIn) {
            assertNull(response.getExpiresIn());
        }

        if (status != 200) {
            assertNotNull(response.getEntity(), "The entity is null");
            assertNotNull(response.getErrorType(), "The error type is null");
            if (errorResponseType != null) {
                assertEquals(errorResponseType, response.getErrorType());
            }
            assertNotNull(response.getErrorDescription(), "The error description is null");
        }
    }
}
