package org.xdi.oxauth.model.discovery;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;

/**
 * OAuth discovery
 *
 * @author yuriyz on 05/17/2017.
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({
        "issuer",
        "authorization_endpoint",
        "token_endpoint",
        "jwks_uri",
        "registration_endpoint",
        "response_types_supported",
        "response_modes_supported",
        "grant_types_supported",
        "token_endpoint_auth_methods_supported",
        "token_endpoint_auth_signing_alg_values_supported",
        "service_documentation",
        "ui_locales_supported",
        "op_policy_uri",
        "op_tos_uri",
        "revocation_endpoint",
        "revocation_endpoint_auth_methods_supported",
        "revocation_endpoint_auth_signing_alg_values_supported",
        "introspection_endpoint",
        "introspection_endpoint_auth_methods_supported",
        "introspection_endpoint_auth_signing_alg_values_supported",
        "code_challenge_methods_supported"
})
@XmlRootElement
@ApiModel(value = "OAuth2 Discovery")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2Discovery {

    @ApiModelProperty(value = "The authorization server's issuer identifier, which is\n a URL that uses the \"https\" scheme and has no query or fragment\n components.  This is the location where \".well-known\" RFC 5785\n [RFC5785] resources containing information about the authorization\n server are published.  Using these well-known resources is\n described in Section 3.  The issuer identifier is used to prevent\n authorization server mix-up attacks, as described in \"OAuth 2.0 Mix-Up Mitigation\" ", required = true)
    @JsonProperty(value = "issuer")
    @XmlElement(name = "issuer")
    private String issuer;

    @ApiModelProperty(value = "URL of the authorization server's authorization endpoint", required = true)
    @JsonProperty(value = "authorization_endpoint")
    @XmlElement(name = "authorization_endpoint")
    private String authorizationEndpoint;

    @ApiModelProperty(value = "URL of the authorization server's token endpoint [RFC6749].  This is REQUIRED unless only the implicit grant type is used", required = true)
    @JsonProperty(value = "token_endpoint")
    @XmlElement(name = "token_endpoint")
    private String tokenEndpoint;

    @ApiModelProperty(value = "URL of the authorization server's JWK Set [JWK] document.  The referenced document contains the signing key(s) the client uses to validate signatures from the authorization server. This URL MUST use the \"https\" scheme.  The JWK Set MAY also contain the server's encryption key(s), which are used by clients to encrypt requests to the server.  When both signing and encryption keys are made available, a \"use\" (public key use) parameter value is REQUIRED for all keys in the referenced JWK Set to indicate each key's intended usage.", required = false)
    @JsonProperty(value = "jwks_uri")
    @XmlElement(name = "jwks_uri")
    private String jwksUri;

    @ApiModelProperty(value = "URL of the authorization server's OAuth 2.0 Dynamic Client Registration endpoint [RFC7591]", required = false)
    @JsonProperty(value = "registration_endpoint")
    @XmlElement(name = "registration_endpoint")
    private String registrationEndpoint;

    @ApiModelProperty(required = true, value = "JSON array containing a list of the OAuth 2.0 \"response_type\" values that this authorization server supports. The array values used are the same as those used with the \"response_types\" parameter defined by \"OAuth 2.0 Dynamic Client Registration Protocol\" [RFC7591]")
    @JsonProperty(value = "response_types_supported")
    @XmlElement(name = "response_types_supported")
    private String[] responseTypesSupported;

//    @ApiModelProperty(required = false, value = "JSON array containing a list of the OAuth 2.0 \"response_mode\" values that this authorization server supports, as specified in OAuth 2.0 Multiple Response Type Encoding Practices [OAuth.Responses].  If omitted, the default is \"[\"query\", \"fragment\"]\".  The response mode value \"form_post\" is also defined in OAuth 2.0 Form Post Response Mode [OAuth.Post")
//    @JsonProperty(value = "response_modes_supported")
//    @XmlElement(name = "response_modes_supported")
//    private String[] responseModesSupported;

    @ApiModelProperty(required = false, value = "JSON array containing a list of the OAuth 2.0 grant type values that this authorization server supports.  The array values used are the same as those used with the \"grant_types\" parameter defined by \"OAuth 2.0 Dynamic Client Registration Protocol\" [RFC7591].  If omitted, the default value is \"[\"authorization_code\", \"implicit\"]\"")
    @JsonProperty(value = "grant_types_supported")
    @XmlElement(name = "grant_types_supported")
    private String[] grantTypesSupported;

    @ApiModelProperty(required = false, value = "JSON array containing a list of client authentication methods supported by this token endpoint.  Client authentication method values are used in the \"token_endpoint_auth_method\" parameter defined in Section 2 of [RFC7591].  If omitted, the default is \"client_secret_basic\" -- the HTTP Basic Authentication Scheme specified in Section 2.3.1 of OAuth 2.0 [RFC6749]")
    @JsonProperty(value = "token_endpoint_auth_methods_supported")
    @XmlElement(name = "token_endpoint_auth_methods_supported")
    private String[] tokenEndpointAuthMethodsSupported;

    @ApiModelProperty(required = false, value = "JSON array containing a list of the JWS signing algorithms (\"alg\" values) supported by the token endpoint for the signature on the JWT [JWT] used to authenticate the client at the token endpoint for the \"private_key_jwt\" and \"client_secret_jwt\" authentication methods.  Servers SHOULD support \"RS256\".  The value \"none\" MUST NOT be used")
    @JsonProperty(value = "token_endpoint_auth_signing_alg_values_supported")
    @XmlElement(name = "token_endpoint_auth_signing_alg_values_supported")
    private String[] tokenEndpointAuthSigningAlgValuesSupported;

    @ApiModelProperty(required = false, value = "URL of a page containing human-readable information that developers might want or need to know when using the authorization server.  In particular, if the authorization server does not support Dynamic Client Registration, then information on how to register clients needs to be provided in this documentation")
    @JsonProperty(value = "service_documentation")
    @XmlElement(name = "service_documentation")
    private String serviceDocumentation;

    @ApiModelProperty(required = false, value = "Languages and scripts supported for the user interface, represented as a JSON array of BCP47 [RFC5646] language tag values.")
    @JsonProperty(value = "ui_locales_supported")
    @XmlElement(name = "ui_locales_supported")
    private String[] uiLocalesSupported;

    @ApiModelProperty(required = false, value = "URL that the authorization server provides to the person registering the client to read about the authorization server's requirements on how the client can use the data provided by the authorization server.  The registration process SHOULD display this URL to the person registering the client if it is given.  As described in Section 5, despite the identifier \"op_policy_uri\", appearing to be OpenID-specific, its usage in this specification is actually referring to a general OAuth 2.0 feature that is not specific to OpenID Connect.")
    @JsonProperty(value = "op_policy_uri")
    @XmlElement(name = "op_policy_uri")
    private String opPolicyUri;

    @ApiModelProperty(required = false, value = "URL that the authorization server provides to the person registering the client to read about the authorization server's terms of service.  The registration process SHOULD display this URL to the person registering the client if it is given.  As described in Section 5, despite the identifier \"op_tos_uri\", appearing to be OpenID-specific, its usage in this specification is actually referring to a general OAuth 2.0 feature that is not specific to OpenID Connect.")
    @JsonProperty(value = "op_tos_uri")
    @XmlElement(name = "op_tos_uri")
    private String opTosUri;

//    @ApiModelProperty(required = false, value = "URL of the authorization server's OAuth 2.0 revocation endpoint [RFC7009]")
//    @JsonProperty(value = "revocation_endpoint")
//    @XmlElement(name = "revocation_endpoint")
//    private String revocationEndpoint;
//
//    @ApiModelProperty(required = false, value = "JSON array containing a list of client authentication methods supported by this revocation endpoint.  The valid client authentication method values are those registered in the IANA \"OAuth Token Endpoint Authentication Methods\" registry [IANA.OAuth.Parameters]")
//    @JsonProperty(value = "revocation_endpoint_auth_methods_supported")
//    @XmlElement(name = "revocation_endpoint_auth_methods_supported")
//    private String[] revocation_endpoint_auth_methods_supported;
//
//    @ApiModelProperty(required = false, value = "JSON array containing a list of the JWS signing algorithms (\"alg\" values) supported by the revocation endpoint for the signature on the JWT [JWT] used to authenticate the client at the revocation endpoint for the \"private_key_jwt\" and \"client_secret_jwt\" authentication methods.  The value \"none\" MUST NOT be used.")
//    @JsonProperty(value = "revocation_endpoint_auth_signing_alg_values_supported")
//    @XmlElement(name = "revocation_endpoint_auth_signing_alg_values_supported")
//    private String[] revocationEndpointAuthSigningAlgValuesSupported;

    @ApiModelProperty(required = false, value = "URL of the authorization server's OAuth 2.0 introspection endpoint [RFC7662].")
    @JsonProperty(value = "introspection_endpoint")
    @XmlElement(name = "introspection_endpoint")
    private String introspectionEndpoint;

//    @ApiModelProperty(required = false, value = "JSON array containing a list of client authentication methods supported by this introspection endpoint.  The valid client authentication method values are those registered in the IANA \"OAuth Token Endpoint Authentication Methods\" registry [IANA.OAuth.Parameters] or those registered in the IANA \"OAuth Access Token Types\" registry [IANA.OAuth.Parameters].  (These values are and will remain distinct, due to Section 7.2.)")
//    @JsonProperty(value = "introspection_endpoint_auth_methods_supported")
//    @XmlElement(name = "introspection_endpoint_auth_methods_supported")
//    private String[] introspectionEndpointAuthMethodsSupported;
//
//    @ApiModelProperty(required = false, value = " JSON array containing a list of the JWS signing algorithms (\"alg\" values) supported by the introspection endpoint for the signature on the JWT [JWT] used to authenticate the client at the introspection endpoint for the \"private_key_jwt\" and \"client_secret_jwt\" authentication methods.  The value \"none\" MUST NOT be used.")
//    @JsonProperty(value = "introspection_endpoint_auth_signing_alg_values_supported")
//    @XmlElement(name = "introspection_endpoint_auth_signing_alg_values_supported")
//    private String[] introspectionEndpointAuthSigningAlgValuesSupported;

    @ApiModelProperty(required = false, value = "JSON array containing a list of PKCE [RFC7636] code challenge methods supported by this authorization server.  Code challenge method values are used in the \"code_challenge_method\" parameter defined in Section 4.3 of [RFC7636].  The valid code challenge method values are those registered in the IANA \"PKCE Code Challenge Methods\" registry [IANA.OAuth.Parameters]")
    @JsonProperty(value = "code_challenge_methods_supported")
    @XmlElement(name = "code_challenge_methods_supported")
    private String[] codeChallengeMethodsSupported;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public String[] getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public void setResponseTypesSupported(String[] responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
    }

//    public String[] getResponseModesSupported() {
//        return responseModesSupported;
//    }
//
//    public void setResponseModesSupported(String[] responseModesSupported) {
//        this.responseModesSupported = responseModesSupported;
//    }

    public String[] getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(String[] grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public String[] getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(String[] tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    public String[] getTokenEndpointAuthSigningAlgValuesSupported() {
        return tokenEndpointAuthSigningAlgValuesSupported;
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(String[] tokenEndpointAuthSigningAlgValuesSupported) {
        this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    public String getServiceDocumentation() {
        return serviceDocumentation;
    }

    public void setServiceDocumentation(String serviceDocumentation) {
        this.serviceDocumentation = serviceDocumentation;
    }

    public String[] getUiLocalesSupported() {
        return uiLocalesSupported;
    }

    public void setUiLocalesSupported(String[] uiLocalesSupported) {
        this.uiLocalesSupported = uiLocalesSupported;
    }

    public String getOpPolicyUri() {
        return opPolicyUri;
    }

    public void setOpPolicyUri(String opPolicyUri) {
        this.opPolicyUri = opPolicyUri;
    }

    public String getOpTosUri() {
        return opTosUri;
    }

    public void setOpTosUri(String opTosUri) {
        this.opTosUri = opTosUri;
    }

//    public String getRevocationEndpoint() {
//        return revocationEndpoint;
//    }
//
//    public void setRevocationEndpoint(String revocationEndpoint) {
//        this.revocationEndpoint = revocationEndpoint;
//    }
//
//    public String[] getRevocation_endpoint_auth_methods_supported() {
//        return revocation_endpoint_auth_methods_supported;
//    }
//
//    public void setRevocation_endpoint_auth_methods_supported(String[] revocation_endpoint_auth_methods_supported) {
//        this.revocation_endpoint_auth_methods_supported = revocation_endpoint_auth_methods_supported;
//    }
//
//    public String[] getRevocationEndpointAuthSigningAlgValuesSupported() {
//        return revocationEndpointAuthSigningAlgValuesSupported;
//    }
//
//    public void setRevocationEndpointAuthSigningAlgValuesSupported(String[] revocationEndpointAuthSigningAlgValuesSupported) {
//        this.revocationEndpointAuthSigningAlgValuesSupported = revocationEndpointAuthSigningAlgValuesSupported;
//    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

//    public String[] getIntrospectionEndpointAuthMethodsSupported() {
//        return introspectionEndpointAuthMethodsSupported;
//    }
//
//    public void setIntrospectionEndpointAuthMethodsSupported(String[] introspectionEndpointAuthMethodsSupported) {
//        this.introspectionEndpointAuthMethodsSupported = introspectionEndpointAuthMethodsSupported;
//    }
//
//    public String[] getIntrospectionEndpointAuthSigningAlgValuesSupported() {
//        return introspectionEndpointAuthSigningAlgValuesSupported;
//    }
//
//    public void setIntrospectionEndpointAuthSigningAlgValuesSupported(String[] introspectionEndpointAuthSigningAlgValuesSupported) {
//        this.introspectionEndpointAuthSigningAlgValuesSupported = introspectionEndpointAuthSigningAlgValuesSupported;
//    }

    public String[] getCodeChallengeMethodsSupported() {
        return codeChallengeMethodsSupported;
    }

    public void setCodeChallengeMethodsSupported(String[] codeChallengeMethodsSupported) {
        this.codeChallengeMethodsSupported = codeChallengeMethodsSupported;
    }

    @Override
    public String toString() {
        return "OAuth2Discovery{" +
                "issuer='" + issuer + '\'' +
                ", authorizationEndpoint='" + authorizationEndpoint + '\'' +
                ", tokenEndpoint='" + tokenEndpoint + '\'' +
                ", jwksUri='" + jwksUri + '\'' +
                ", registrationEndpoint='" + registrationEndpoint + '\'' +
                ", responseTypesSupported=" + Arrays.toString(responseTypesSupported) +
//                ", responseModesSupported=" + Arrays.toString(responseModesSupported) +
                ", grantTypesSupported=" + Arrays.toString(grantTypesSupported) +
                ", tokenEndpointAuthMethodsSupported=" + Arrays.toString(tokenEndpointAuthMethodsSupported) +
                ", tokenEndpointAuthSigningAlgValuesSupported=" + Arrays.toString(tokenEndpointAuthSigningAlgValuesSupported) +
                ", serviceDocumentation='" + serviceDocumentation + '\'' +
                ", uiLocalesSupported=" + Arrays.toString(uiLocalesSupported) +
                ", opPolicyUri='" + opPolicyUri + '\'' +
                ", opTosUri='" + opTosUri + '\'' +
//                ", revocationEndpoint='" + revocationEndpoint + '\'' +
//                ", revocation_endpoint_auth_methods_supported=" + Arrays.toString(revocation_endpoint_auth_methods_supported) +
//                ", revocationEndpointAuthSigningAlgValuesSupported=" + Arrays.toString(revocationEndpointAuthSigningAlgValuesSupported) +
                ", introspectionEndpoint='" + introspectionEndpoint + '\'' +
//                ", introspectionEndpointAuthMethodsSupported=" + Arrays.toString(introspectionEndpointAuthMethodsSupported) +
//                ", introspectionEndpointAuthSigningAlgValuesSupported=" + Arrays.toString(introspectionEndpointAuthSigningAlgValuesSupported) +
                ", codeChallengeMethodsSupported=" + Arrays.toString(codeChallengeMethodsSupported) +
                '}';
    }
}
