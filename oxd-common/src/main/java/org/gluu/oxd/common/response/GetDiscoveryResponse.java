package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetDiscoveryResponse implements IOpResponse {

    @JsonProperty(value = "issuer")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuer")
    private String issuer;
    @JsonProperty(value = "authorization_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorization_endpoint")
    private String authorization_endpoint;
    @JsonProperty(value = "token_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "token_endpoint")
    private String token_endpoint;
    @JsonProperty(value = "token_revocation_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "token_revocation_endpoint")
    private String token_revocation_endpoint;
    @JsonProperty(value = "user_info_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "user_info_endpoint")
    private String user_info_endpoint;
    @JsonProperty(value = "client_info_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_info_endpoint")
    private String client_info_endpoint;
    @JsonProperty(value = "check_session_iframe")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "check_session_iframe")
    private String check_session_iframe;
    @JsonProperty(value = "end_session_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "end_session_endpoint")
    private String end_session_endpoint;
    @JsonProperty(value = "jwks_uri")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "jwks_uri")
    private String jwks_uri;
    @JsonProperty(value = "registration_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "registration_endpoint")
    private String registration_endpoint;
    @JsonProperty(value = "id_generation_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id_generation_endpoint")
    private String id_generation_endpoint;
    @JsonProperty(value = "introspection_endpoint")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "introspection_endpoint")
    private String introspection_endpoint;
    @JsonProperty(value = "scopes_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "scopes_supported")
    private List<String> scopes_supported;
    @JsonProperty(value = "response_types_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "response_types_supported")
    private List<String> response_types_supported;
    @JsonProperty(value = "grant_types_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "grant_types_supported")
    private List<String> grant_types_supported;
    @JsonProperty(value = "acr_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "acr_values_supported")
    private List<String> acr_values_supported;
    @JsonProperty(value = "subject_types_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "subject_types_supported")
    private List<String> subject_types_supported;
    @JsonProperty(value = "user_info_signing_alg_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "user_info_signing_alg_values_supported")
    private List<String> user_info_signing_alg_values_supported;
    @JsonProperty(value = "user_info_encryption_alg_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "user_info_encryption_alg_values_supported")
    private List<String> user_info_encryption_alg_values_supported;
    @JsonProperty(value = "user_info_encryption_enc_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "user_info_encryption_enc_values_supported")
    private List<String> user_info_encryption_enc_values_supported;
    @JsonProperty(value = "id_token_signing_alg_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id_token_signing_alg_values_supported")
    private List<String> id_token_signing_alg_values_supported;
    @JsonProperty(value = "id_token_encryption_alg_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id_token_encryption_alg_values_supported")
    private List<String> id_token_encryption_alg_values_supported;
    @JsonProperty(value = "id_token_encryption_enc_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id_token_encryption_enc_values_supported")
    private List<String> id_token_encryption_enc_values_supported;
    @JsonProperty(value = "request_object_signing_alg_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request_object_signing_alg_values_supported")
    private List<String> request_object_signing_alg_values_supported;
    @JsonProperty(value = "request_object_encryption_alg_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request_object_encryption_alg_values_supported")
    private List<String> request_object_encryption_alg_values_supported;
    @JsonProperty(value = "request_object_encryption_enc_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request_object_encryption_enc_values_supported")
    private List<String> request_object_encryption_enc_values_supported;
    @JsonProperty(value = "token_endpoint_auth_methods_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "token_endpoint_auth_methods_supported")
    private List<String> token_endpoint_auth_methods_supported;
    @JsonProperty(value = "token_endpoint_auth_signing_alg_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "token_endpoint_auth_signing_alg_values_supported")
    private List<String> token_endpoint_auth_signing_alg_values_supported;
    @JsonProperty(value = "display_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "display_values_supported")
    private List<String> display_values_supported;
    @JsonProperty(value = "claim_types_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "claim_types_supported")
    private List<String> claim_types_supported;
    @JsonProperty(value = "claims_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "claims_supported")
    private List<String> claims_supported;
    @JsonProperty(value = "id_token_token_binding_cnf_values_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id_token_token_binding_cnf_values_supported")
    private List<String> id_token_token_binding_cnf_values_supported;
    @JsonProperty(value = "service_documentation")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "service_documentation")
    private String service_documentation;
    @JsonProperty(value = "claims_locales_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "claims_locales_supported")
    private List<String> claims_locales_supported;
    @JsonProperty(value = "ui_locales_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ui_locales_supported")
    private List<String> ui_locales_supported;
    @JsonProperty(value = "claims_parameter_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "claims_parameter_supported")
    private Boolean claims_parameter_supported;
    @JsonProperty(value = "request_parameter_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request_parameter_supported")
    private Boolean request_parameter_supported;
    @JsonProperty(value = "request_uri_parameter_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request_uri_parameter_supported")
    private Boolean request_uri_parameter_supported;
    @JsonProperty(value = "require_request_uri_registration")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "require_request_uri_registration")
    private Boolean require_request_uri_registration;
    @JsonProperty(value = "tls_client_certificate_bound_access_tokens")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tls_client_certificate_bound_access_tokens")
    private Boolean tls_client_certificate_bound_access_tokens;
    @JsonProperty(value = "front_channel_logout_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "front_channel_logout_supported")
    private Boolean front_channel_logout_supported;
    @JsonProperty(value = "front_channel_logout_session_supported")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "front_channel_logout_session_supported")
    private Boolean front_channel_logout_session_supported;
    @JsonProperty(value = "op_policy_uri")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "op_policy_uri")
    private String op_policy_uri;
    @JsonProperty(value = "op_tos_uri")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "op_tos_uri")
    private String op_tos_uri;
    @JsonProperty(value = "scope_to_claims_mapping")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "scope_to_claims_mapping")
    private Map<String, List<String>> scope_to_claims_mapping = new HashMap<String, List<String>>();

    public GetDiscoveryResponse() {
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAuthorizationEndpoint() {
        return authorization_endpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorization_endpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return token_endpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.token_endpoint = tokenEndpoint;
    }

    public String getTokenRevocationEndpoint() {
        return token_revocation_endpoint;
    }

    public void setTokenRevocationEndpoint(String tokenRevocationEndpoint) {
        this.token_revocation_endpoint = tokenRevocationEndpoint;
    }

    public String getUserInfoEndpoint() {
        return user_info_endpoint;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.user_info_endpoint = userInfoEndpoint;
    }

    public String getClientInfoEndpoint() {
        return client_info_endpoint;
    }

    public void setClientInfoEndpoint(String clientInfoEndpoint) {
        this.client_info_endpoint = clientInfoEndpoint;
    }

    public String getCheckSessionIFrame() {
        return check_session_iframe;
    }

    public void setCheckSessionIFrame(String checkSessionIFrame) {
        this.check_session_iframe = checkSessionIFrame;
    }

    public String getEndSessionEndpoint() {
        return end_session_endpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.end_session_endpoint = endSessionEndpoint;
    }

    public String getJwksUri() {
        return jwks_uri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwks_uri = jwksUri;
    }

    public String getRegistrationEndpoint() {
        return registration_endpoint;
    }

    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registration_endpoint = registrationEndpoint;
    }

    public String getIdGenerationEndpoint() {
        return id_generation_endpoint;
    }

    public void setIdGenerationEndpoint(String idGenerationEndpoint) {
        this.id_generation_endpoint = idGenerationEndpoint;
    }

    public String getIntrospectionEndpoint() {
        return introspection_endpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspection_endpoint = introspectionEndpoint;
    }

    public List<String> getScopesSupported() {
        return scopes_supported;
    }

    public void setScopesSupported(List<String> scopesSupported) {
        this.scopes_supported = scopesSupported;
    }

    public List<String> getResponseTypesSupported() {
        return response_types_supported;
    }

    public void setResponseTypesSupported(List<String> responseTypesSupported) {
        this.response_types_supported = responseTypesSupported;
    }

    public List<String> getGrantTypesSupported() {
        return grant_types_supported;
    }

    public void setGrantTypesSupported(List<String> grantTypesSupported) {
        this.grant_types_supported = grantTypesSupported;
    }

    public List<String> getAcrValuesSupported() {
        return acr_values_supported;
    }

    public void setAcrValuesSupported(List<String> acrValuesSupported) {
        this.acr_values_supported = acrValuesSupported;
    }

    public List<String> getSubjectTypesSupported() {
        return subject_types_supported;
    }

    public void setSubjectTypesSupported(List<String> subjectTypesSupported) {
        this.subject_types_supported = subjectTypesSupported;
    }

    public List<String> getUserInfoSigningAlgValuesSupported() {
        return user_info_signing_alg_values_supported;
    }

    public void setUserInfoSigningAlgValuesSupported(List<String> userInfoSigningAlgValuesSupported) {
        this.user_info_signing_alg_values_supported = userInfoSigningAlgValuesSupported;
    }

    public List<String> getUserInfoEncryptionAlgValuesSupported() {
        return user_info_encryption_alg_values_supported;
    }

    public void setUserInfoEncryptionAlgValuesSupported(List<String> userInfoEncryptionAlgValuesSupported) {
        this.user_info_encryption_alg_values_supported = userInfoEncryptionAlgValuesSupported;
    }

    public List<String> getUserInfoEncryptionEncValuesSupported() {
        return user_info_encryption_enc_values_supported;
    }

    public void setUserInfoEncryptionEncValuesSupported(List<String> userInfoEncryptionEncValuesSupported) {
        this.user_info_encryption_enc_values_supported = userInfoEncryptionEncValuesSupported;
    }

    public List<String> getIdTokenSigningAlgValuesSupported() {
        return id_token_signing_alg_values_supported;
    }

    public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) {
        this.id_token_signing_alg_values_supported = idTokenSigningAlgValuesSupported;
    }

    public List<String> getIdTokenEncryptionAlgValuesSupported() {
        return id_token_encryption_alg_values_supported;
    }

    public void setIdTokenEncryptionAlgValuesSupported(List<String> idTokenEncryptionAlgValuesSupported) {
        this.id_token_encryption_alg_values_supported = idTokenEncryptionAlgValuesSupported;
    }

    public List<String> getIdTokenEncryptionEncValuesSupported() {
        return id_token_encryption_enc_values_supported;
    }

    public void setIdTokenEncryptionEncValuesSupported(List<String> idTokenEncryptionEncValuesSupported) {
        this.id_token_encryption_enc_values_supported = idTokenEncryptionEncValuesSupported;
    }

    public List<String> getRequestObjectSigningAlgValuesSupported() {
        return request_object_signing_alg_values_supported;
    }

    public void setRequestObjectSigningAlgValuesSupported(List<String> requestObjectSigningAlgValuesSupported) {
        this.request_object_signing_alg_values_supported = requestObjectSigningAlgValuesSupported;
    }

    public List<String> getRequestObjectEncryptionAlgValuesSupported() {
        return request_object_encryption_alg_values_supported;
    }

    public void setRequestObjectEncryptionAlgValuesSupported(List<String> requestObjectEncryptionAlgValuesSupported) {
        this.request_object_encryption_alg_values_supported = requestObjectEncryptionAlgValuesSupported;
    }

    public List<String> getRequestObjectEncryptionEncValuesSupported() {
        return request_object_encryption_enc_values_supported;
    }

    public void setRequestObjectEncryptionEncValuesSupported(List<String> requestObjectEncryptionEncValuesSupported) {
        this.request_object_encryption_enc_values_supported = requestObjectEncryptionEncValuesSupported;
    }

    public List<String> getTokenEndpointAuthMethodsSupported() {
        return token_endpoint_auth_methods_supported;
    }

    public void setTokenEndpointAuthMethodsSupported(List<String> tokenEndpointAuthMethodsSupported) {
        this.token_endpoint_auth_methods_supported = tokenEndpointAuthMethodsSupported;
    }

    public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return token_endpoint_auth_signing_alg_values_supported;
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(List<String> tokenEndpointAuthSigningAlgValuesSupported) {
        this.token_endpoint_auth_signing_alg_values_supported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    public List<String> getDisplayValuesSupported() {
        return display_values_supported;
    }

    public void setDisplayValuesSupported(List<String> displayValuesSupported) {
        this.display_values_supported = displayValuesSupported;
    }

    public List<String> getClaimTypesSupported() {
        return claim_types_supported;
    }

    public void setClaimTypesSupported(List<String> claimTypesSupported) {
        this.claim_types_supported = claimTypesSupported;
    }

    public List<String> getClaimsSupported() {
        return claims_supported;
    }

    public void setClaimsSupported(List<String> claimsSupported) {
        this.claims_supported = claimsSupported;
    }

    public List<String> getIdTokenTokenBindingCnfValuesSupported() {
        return id_token_token_binding_cnf_values_supported;
    }

    public void setIdTokenTokenBindingCnfValuesSupported(List<String> idTokenTokenBindingCnfValuesSupported) {
        this.id_token_token_binding_cnf_values_supported = idTokenTokenBindingCnfValuesSupported;
    }

    public String getServiceDocumentation() {
        return service_documentation;
    }

    public void setServiceDocumentation(String serviceDocumentation) {
        this.service_documentation = serviceDocumentation;
    }

    public List<String> getClaimsLocalesSupported() {
        return claims_locales_supported;
    }

    public void setClaimsLocalesSupported(List<String> claimsLocalesSupported) {
        this.claims_locales_supported = claimsLocalesSupported;
    }

    public List<String> getUiLocalesSupported() {
        return ui_locales_supported;
    }

    public void setUiLocalesSupported(List<String> uiLocalesSupported) {
        this.ui_locales_supported = uiLocalesSupported;
    }

    public Boolean getClaimsParameterSupported() {
        return claims_parameter_supported;
    }

    public void setClaimsParameterSupported(Boolean claimsParameterSupported) {
        this.claims_parameter_supported = claimsParameterSupported;
    }

    public Boolean getRequestParameterSupported() {
        return request_parameter_supported;
    }

    public void setRequestParameterSupported(Boolean requestParameterSupported) {
        this.request_parameter_supported = requestParameterSupported;
    }

    public Boolean getRequestUriParameterSupported() {
        return request_uri_parameter_supported;
    }

    public void setRequestUriParameterSupported(Boolean requestUriParameterSupported) {
        this.request_uri_parameter_supported = requestUriParameterSupported;
    }

    public Boolean getRequireRequestUriRegistration() {
        return require_request_uri_registration;
    }

    public void setRequireRequestUriRegistration(Boolean requireRequestUriRegistration) {
        this.require_request_uri_registration = requireRequestUriRegistration;
    }

    public Boolean getTlsClientCertificateBoundAccessTokens() {
        return tls_client_certificate_bound_access_tokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Boolean tlsClientCertificateBoundAccessTokens) {
        this.tls_client_certificate_bound_access_tokens = tlsClientCertificateBoundAccessTokens;
    }

    public Boolean getFrontChannelLogoutSupported() {
        return front_channel_logout_supported;
    }

    public void setFrontChannelLogoutSupported(Boolean frontChannelLogoutSupported) {
        this.front_channel_logout_supported = frontChannelLogoutSupported;
    }

    public Boolean getFrontChannelLogoutSessionSupported() {
        return front_channel_logout_session_supported;
    }

    public void setFrontChannelLogoutSessionSupported(Boolean frontChannelLogoutSessionSupported) {
        this.front_channel_logout_session_supported = frontChannelLogoutSessionSupported;
    }

    public String getOpPolicyUri() {
        return op_policy_uri;
    }

    public void setOpPolicyUri(String opPolicyUri) {
        this.op_policy_uri = opPolicyUri;
    }

    public String getOpTosUri() {
        return op_tos_uri;
    }

    public void setOpTosUri(String opTosUri) {
        this.op_tos_uri = opTosUri;
    }

    public Map<String, List<String>> getScopeToClaimsMapping() {
        return scope_to_claims_mapping;
    }

    public void setScopeToClaimsMapping(Map<String, List<String>> scopeToClaimsMapping) {
        this.scope_to_claims_mapping = scopeToClaimsMapping;
    }

    @Override
    public String toString() {
        return "GetDiscoveryResponse{" +
                "issuer='" + issuer + '\'' +
                ", authorization_endpoint='" + authorization_endpoint + '\'' +
                ", token_endpoint='" + token_endpoint + '\'' +
                ", token_revocation_endpoint='" + token_revocation_endpoint + '\'' +
                ", user_info_endpoint='" + user_info_endpoint + '\'' +
                ", client_info_endpoint='" + client_info_endpoint + '\'' +
                ", check_session_iframe='" + check_session_iframe + '\'' +
                ", end_session_endpoint='" + end_session_endpoint + '\'' +
                ", jwks_uri='" + jwks_uri + '\'' +
                ", registration_endpoint='" + registration_endpoint + '\'' +
                ", id_generation_endpoint='" + id_generation_endpoint + '\'' +
                ", introspection_endpoint='" + introspection_endpoint + '\'' +
                ", scopes_supported=" + scopes_supported +
                ", response_types_supported=" + response_types_supported +
                ", grant_types_supported=" + grant_types_supported +
                ", acr_values_supported=" + acr_values_supported +
                ", subject_types_supported=" + subject_types_supported +
                ", user_info_signing_alg_values_supported=" + user_info_signing_alg_values_supported +
                ", user_info_encryption_alg_values_supported=" + user_info_encryption_alg_values_supported +
                ", user_info_encryption_enc_values_supported=" + user_info_encryption_enc_values_supported +
                ", id_token_signing_alg_values_supported=" + id_token_signing_alg_values_supported +
                ", id_token_encryption_alg_values_supported=" + id_token_encryption_alg_values_supported +
                ", id_token_encryption_enc_values_supported=" + id_token_encryption_enc_values_supported +
                ", request_object_signing_alg_values_supported=" + request_object_signing_alg_values_supported +
                ", request_object_encryption_alg_values_supported=" + request_object_encryption_alg_values_supported +
                ", request_object_encryption_enc_values_supported=" + request_object_encryption_enc_values_supported +
                ", token_endpoint_auth_methods_supported=" + token_endpoint_auth_methods_supported +
                ", token_endpoint_auth_signing_alg_values_supported=" + token_endpoint_auth_signing_alg_values_supported +
                ", display_values_supported=" + display_values_supported +
                ", claim_types_supported=" + claim_types_supported +
                ", claims_supported=" + claims_supported +
                ", id_token_token_binding_cnf_values_supported=" + id_token_token_binding_cnf_values_supported +
                ", service_documentation='" + service_documentation + '\'' +
                ", claims_locales_supported=" + claims_locales_supported +
                ", ui_locales_supported=" + ui_locales_supported +
                ", claims_parameter_supported=" + claims_parameter_supported +
                ", request_parameter_supported=" + request_parameter_supported +
                ", request_uri_parameter_supported=" + request_uri_parameter_supported +
                ", require_request_uri_registration=" + require_request_uri_registration +
                ", tls_client_certificate_bound_access_tokens=" + tls_client_certificate_bound_access_tokens +
                ", front_channel_logout_supported=" + front_channel_logout_supported +
                ", front_channel_logout_session_supported=" + front_channel_logout_session_supported +
                ", op_policy_uri='" + op_policy_uri + '\'' +
                ", op_tos_uri='" + op_tos_uri + '\'' +
                ", scope_to_claims_mapping=" + scope_to_claims_mapping +
                '}';
    }
}
