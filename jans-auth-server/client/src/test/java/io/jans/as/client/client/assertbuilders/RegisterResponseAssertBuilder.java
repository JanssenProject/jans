package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.RegisterResponse;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import org.apache.commons.lang3.BooleanUtils;

import static io.jans.as.model.register.RegisterRequestParam.*;
import static org.testng.Assert.*;

public class RegisterResponseAssertBuilder extends BaseAssertBuilder {

    private RegisterResponse response;
    private int status;
    private boolean notNullRegistrationClientUri;
    private AsymmetricSignatureAlgorithm backchannelRequestSigningAlgorithm;
    private BackchannelTokenDeliveryMode backchannelTokenDeliveryMode;
    private Boolean backchannelUserCodeParameter;

    public RegisterResponseAssertBuilder(RegisterResponse response) {
        this.response = response;
        this.notNullRegistrationClientUri = false;
        this.status = 200;
        this.backchannelRequestSigningAlgorithm = null;
        this.backchannelTokenDeliveryMode = null;
        this.backchannelUserCodeParameter = null;
    }

    public RegisterResponseAssertBuilder ok() {
        this.status = 200;
        return this;
    }

    public RegisterResponseAssertBuilder created() {
        this.status = 201;
        return this;
    }

    public RegisterResponseAssertBuilder bad() {
        this.status = 400;
        return this;
    }

    public RegisterResponseAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public RegisterResponseAssertBuilder notNullRegistrationClientUri() {
        this.notNullRegistrationClientUri = true;
        return this;
    }

    public RegisterResponseAssertBuilder backchannelRequestSigningAlgorithm(AsymmetricSignatureAlgorithm backchannelRequestSigningAlgorithm) {
        this.backchannelRequestSigningAlgorithm = backchannelRequestSigningAlgorithm;
        return this;
    }

    public RegisterResponseAssertBuilder backchannelTokenDeliveryMode(BackchannelTokenDeliveryMode backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
        return this;
    }

    public RegisterResponseAssertBuilder backchannelUserCodeParameter(Boolean backchannelUserCodeParameter) {
        this.backchannelUserCodeParameter = backchannelUserCodeParameter;
        return this;
    }

    @Override
    public void check() {
        if (status == 200 || status == 201) {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());

            assertNotNull(response.getClientId());
            assertNotNull(response.getClientSecret());
            assertNotNull(response.getClientSecretExpiresAt());
            assertNotNull(response.getRegistrationAccessToken());
            assertNotNull(response.getClientIdIssuedAt());

            if (notNullRegistrationClientUri || status == 201) { // when created status it must be always present
                assertNotNull(response.getRegistrationClientUri());//Review usage
            }
            //Backchannel claims
            if (backchannelRequestSigningAlgorithm != null) {
                assertTrue(response.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
                assertEquals(response.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), backchannelRequestSigningAlgorithm.getValue());
            }
            if (BooleanUtils.isTrue(backchannelUserCodeParameter)) {
                assertTrue(response.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
                assertEquals(response.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), String.valueOf(backchannelUserCodeParameter));
            }
            if (backchannelTokenDeliveryMode != null) {
                assertTrue(response.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
                assertEquals(response.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), backchannelTokenDeliveryMode.getValue());
                if (backchannelTokenDeliveryMode.equals(BackchannelTokenDeliveryMode.PING) || backchannelTokenDeliveryMode.equals(BackchannelTokenDeliveryMode.PUSH)) {
                    assertTrue(response.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
                }
            }
        } else {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getEntity(), "The entity is null");
            assertNotNull(response.getErrorType(), "The error type is null");
            assertNotNull(response.getErrorDescription(), "The error description is null");
        }
    }

}
