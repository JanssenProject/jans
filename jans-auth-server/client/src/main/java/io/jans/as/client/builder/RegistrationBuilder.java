package io.jans.as.client.builder;

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

import jakarta.ws.rs.HttpMethod;
import java.util.List;

public class RegistrationBuilder implements Builder {

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

    private boolean isUpdateMode;
    private boolean isReadMode;

    public RegistrationBuilder() {
        this.applicationType = ApplicationType.WEB;
        this.clientName = "jans test app";
    }

    public RegistrationBuilder withRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
        return this;
    }

    public RegistrationBuilder withApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
        return this;
    }

    public RegistrationBuilder withClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public RegistrationBuilder withRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
        return this;
    }

    public RegistrationBuilder withJwks(String jwks) {
        this.jwks = jwks;
        return this;
    }

    public RegistrationBuilder withJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
        return this;
    }

    public RegistrationBuilder missingJwksUri() {
        this.jwksUri = null;
        return this;
    }

    public RegistrationBuilder withSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
        return this;
    }

    public RegistrationBuilder withGrantTypes(List<GrantType> grantTypeList) {
        this.grantTypeList = grantTypeList;
        return this;
    }

    public RegistrationBuilder withSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
        return this;
    }

    public RegistrationBuilder withRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
        return this;
    }

    public RegistrationBuilder withBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
        return this;
    }

    public RegistrationBuilder missingBackchannelTokenDeliveryMode() {
        this.backchannelTokenDeliveryMode = null;
        return this;
    }

    public RegistrationBuilder withBackchannelClientNotificationEndPoint(String backchannelClientNotificationEndPoint) {
        this.backchannelClientNotificationEndPoint = backchannelClientNotificationEndPoint;
        return this;
    }

    public RegistrationBuilder missingBackchannelClientNotificationEndPoint() {
        this.backchannelClientNotificationEndPoint = null;
        return this;
    }

    public RegistrationBuilder withBackchannelAuthRequestSigningAlgorithm(AsymmetricSignatureAlgorithm backchannekAuthRequestSigningAlgorithm) {
        this.backchannekAuthRequestSigningAlgorithm = backchannekAuthRequestSigningAlgorithm;
        return this;
    }

    public RegistrationBuilder withBackchannelUserCodeParameter(Boolean backchannelUserCodeParameter) {
        this.backchannelUserCodeParameter = backchannelUserCodeParameter;
        return this;
    }

    public RegistrationBuilder withTokenEndPointAuthMethod(AuthenticationMethod tokenEndPointAuthenticationMethod) {
        this.tokenEndpointAuthMethod = tokenEndPointAuthenticationMethod;
        return this;
    }

    public RegistrationBuilder missingTokenEndPointAuthMethod() {
        this.tokenEndpointAuthMethod = null;
        return this;
    }

    public RegistrationBuilder withTokenEndPointAuthSigningAlgorithm(SignatureAlgorithm tokenEndpointAuthSigningAlgorithm) {
        this.tokenEndpointAuthSigningAlgorithm = tokenEndpointAuthSigningAlgorithm;
        return this;
    }

    public RegistrationBuilder withTokenSignedResponseAlgorithm(SignatureAlgorithm tokenSignedResponseAlgorithm) {
        this.tokenSignedResponseAlgorithm = tokenSignedResponseAlgorithm;
        return this;
    }

    public RegistrationBuilder isUpdateMode() {
        this.isUpdateMode = true;
        this.isReadMode = false;
        return this;
    }

    public RegistrationBuilder isReadMode() {
        this.isReadMode = true;
        this.isUpdateMode = false;
        return this;
    }

    @Override
    public RegisterResponse execute() {
        RegisterRequest registerRequest;
        if (isReadMode || isUpdateMode) {
            registerRequest = new RegisterRequest(registrationAccessToken);
        } else {
            registerRequest = new RegisterRequest(applicationType, clientName, redirectUris);
        }
        if (isUpdateMode) {
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
        if (jwks != null) {
            registerRequest.setJwks(jwks);
        }

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();
        ClientUtils.showClient(registerClient);
        return response;
    }

}
