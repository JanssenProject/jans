package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.BackchannelAuthenticationResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;

import static org.testng.Assert.*;

public class BackchannelAuthenticationResponseAssertBuilder extends AssertBuilder {

    private BackchannelAuthenticationResponse response;
    private int status;
    private boolean notNullInterval;
    private boolean nullInterval;
    private BackchannelAuthenticationErrorResponseType errorResponseType;

    public BackchannelAuthenticationResponseAssertBuilder(BackchannelAuthenticationResponse response) {
        this.response = response;
        this.status = 200;
        this.notNullInterval = false;
        this.nullInterval = false;
        this.errorResponseType = null;
    }

    public BackchannelAuthenticationResponseAssertBuilder status(int status) {
        this.status = status;
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
    public void checkAsserts() {
        if (status == 200) {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getAuthReqId());
            assertNotNull(response.getExpiresIn());
            if (notNullInterval) {
                assertNotNull(response.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.
            } else if (nullInterval) {
                assertNull(response.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.
            }
        } else {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getEntity(), "The entity is null");
            assertNotNull(response.getErrorType(), "The error type is null");
            if (errorResponseType != null) {
                assertEquals(errorResponseType, response.getErrorType());
            }
            assertNotNull(response.getErrorDescription(), "The error description is null");
        }
    }
}
