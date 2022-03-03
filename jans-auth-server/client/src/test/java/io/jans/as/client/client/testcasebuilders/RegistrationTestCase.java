package io.jans.as.client.client.testcasebuilders;

import io.jans.as.client.ClientUtils;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;

import javax.ws.rs.HttpMethod;
import java.util.List;

public class RegistrationTestCase extends BaseTestCase {

    private RegisterResponse response;
    private ApplicationType applicationType;
    private SubjectType subjectType;
    private String clientName;
    private String jwks;
    private List<String> redirectUris;
    private String registrationEndpoint;
    private String sectorIdentifierUri;
    private String jwksUri;
    private String backchannelClientNotificationEndPoint;
    private BackchannelTokenDeliveryMode backchannelTokenDeliveryMode;
    private AsymmetricSignatureAlgorithm backchannekAuthRequestSigningAlgorithm;
    private AuthenticationMethod tokenEndpointAuthMethod;
    private Boolean backchannelUserCodeParameter;
    private SignatureAlgorithm tokenSignedResponseAlgorithm;
    private List<GrantType> grantTypeList;
    private SignatureAlgorithm tokenEndpointAuthSigningAlgorithm;
    private String registrationAccessToken;

    private boolean isUpdateMode = false;
    private boolean isReadMode = false;

    public RegistrationTestCase(String title) {
        super(title);
        this.applicationType = ApplicationType.WEB;
        this.clientName = "jans test app";
        this.redirectUris = null;
        this.isUpdateMode = false;
        this.isReadMode = false;
    }

    public RegistrationTestCase withRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
        return this;
    }

    public RegistrationTestCase withApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
        return this;
    }

    public RegistrationTestCase withClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public RegistrationTestCase withRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
        return this;
    }

    public RegistrationTestCase withJwks(String jwks) {
        this.jwks = jwks;
        return this;
    }

    public RegistrationTestCase withJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
        return this;
    }

    public RegistrationTestCase withSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
        return this;
    }

    public RegistrationTestCase withGrantTypes(List<GrantType> grantTypeList) {
        this.grantTypeList = grantTypeList;
        return this;
    }

    public RegistrationTestCase withSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
        return this;
    }

    public RegistrationTestCase withRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
        return this;
    }

    public RegistrationTestCase withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
        return this;
    }

    public RegistrationTestCase withBackchannelClientNotificationEndPoint(String backchannelClientNotificationEndPoint) {
        this.backchannelClientNotificationEndPoint = backchannelClientNotificationEndPoint;
        return this;
    }

    public RegistrationTestCase withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm backchannekAuthRequestSigningAlgorithm) {
        this.backchannekAuthRequestSigningAlgorithm = backchannekAuthRequestSigningAlgorithm;
        return this;
    }

    public RegistrationTestCase withBackchannelUserCodeParameter(Boolean backchannelUserCodeParameter) {
        this.backchannelUserCodeParameter = backchannelUserCodeParameter;
        return this;
    }

    public RegistrationTestCase withTokenEndPointAuthMethod(AuthenticationMethod tokenEndPointAuthenticationMethod) {
        this.tokenEndpointAuthMethod = tokenEndPointAuthenticationMethod;
        return this;
    }

    public RegistrationTestCase withTokenEndPointAuthSigningAlgorithm(SignatureAlgorithm tokenEndpointAuthSigningAlgorithm) {
        this.tokenEndpointAuthSigningAlgorithm = tokenEndpointAuthSigningAlgorithm;
        return this;
    }

    public RegistrationTestCase withTokenSignedResponseAlgorithm(SignatureAlgorithm tokenSignedResponseAlgorithm) {
        this.tokenSignedResponseAlgorithm = tokenSignedResponseAlgorithm;
        return this;
    }

    public RegistrationTestCase isUpdateMode() {
        this.isUpdateMode = true;
        this.isReadMode = false;
        return this;
    }

    public RegistrationTestCase isReadMode() {
        this.isReadMode = true;
        this.isUpdateMode = false;
        return this;
    }


    @Override
    public RegisterResponse excuteTestCase() {
        RegisterRequest registerRequest;
        registerRequest = new RegisterRequest(applicationType, clientName, redirectUris);

        if (isReadMode) {
            registerRequest = new RegisterRequest(registrationAccessToken);
        } else if (isUpdateMode) {
            registerRequest.setHttpMethod(HttpMethod.PUT);
        }

        if (subjectType != null) {
            registerRequest.setSubjectType(subjectType);
        }
        if (jwksUri != null) {
            registerRequest.setJwksUri(jwksUri);
        }
        if (grantTypeList != null) {
            registerRequest.setGrantTypes(grantTypeList);
        }
        if (sectorIdentifierUri != null) {
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        }
        if (backchannelUserCodeParameter != null) {
            registerRequest.setBackchannelUserCodeParameter(backchannelUserCodeParameter);
        }

        if (backchannelTokenDeliveryMode != null) {
            registerRequest.setBackchannelTokenDeliveryMode(backchannelTokenDeliveryMode);
        }
        if (backchannelClientNotificationEndPoint != null) {
            registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndPoint);
        }
        if (backchannekAuthRequestSigningAlgorithm != null) {
            registerRequest.setBackchannelAuthenticationRequestSigningAlg(backchannekAuthRequestSigningAlgorithm);
        }

        if (tokenEndpointAuthMethod != null) {
            registerRequest.setTokenEndpointAuthMethod(tokenEndpointAuthMethod);
        }
        if (tokenSignedResponseAlgorithm != null) {
            registerRequest.setIdTokenSignedResponseAlg(tokenSignedResponseAlgorithm);
        }
        if (tokenEndpointAuthSigningAlgorithm != null) {
            registerRequest.setTokenEndpointAuthSigningAlg(tokenEndpointAuthSigningAlgorithm);
        }

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        response = registerClient.exec();
        ClientUtils.showClient(registerClient);
        return response;
    }

}
