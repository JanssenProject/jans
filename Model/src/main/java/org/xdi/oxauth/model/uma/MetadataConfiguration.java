package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;

/**
 * UMA metadata configuration
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *         Date: 10/25/2012
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"version", "issuer",
        "pat_profiles_supported", "aat_profiles_supported", "rptProfilesSupported",
        "pat_grant_types_supported", "aat_grant_types_supported", "claim_profiles_supported",
        "dynamic_client_endpoint", "token_endpoint", "user_endpoint", "introspection_endpoint",
        "resource_set_registration_endpoint", "permission_registration_endpoint", "rpt_endpoint",
        "authorization_request_endpoint, scope_endpoint"})
@XmlRootElement
public class MetadataConfiguration {

    private String version;

    private String issuer;

    private String[] patProfilesSupported;

    private String[] aatProfilesSupported;

    private String[] rptProfilesSupported;

    private String[] patGrantTypesSupported;

    private String[] aatGrantTypesSupported;

    private String[] claimProfilesSupported;

    private String dynamicClientEndpoint;

    private String tokenEndpoint;

    private String userEndpoint;

    private String resourceSetRegistrationEndpoint;

    private String introspectionEndpoint;

    private String permissionRegistrationEndpoint;

    private String rptEndpoint;

    private String authorizationRequestEndpoint;

    private String scopeEndpoint;

    @JsonProperty(value = "scope_endpoint")
    @XmlElement(name = "scope_endpoint")
    public String getScopeEndpoint() {
        return scopeEndpoint;
    }

    public void setScopeEndpoint(String p_scopeEndpoint) {
        scopeEndpoint = p_scopeEndpoint;
    }

    @JsonProperty(value = "pat_profiles_supported")
    @XmlElement(name = "pat_profiles_supported")
    public String[] getPatProfilesSupported() {
        return patProfilesSupported;
    }

    public void setPatProfilesSupported(String[] p_patProfilesSupported) {
        patProfilesSupported = p_patProfilesSupported;
    }

    @JsonProperty(value = "version")
    @XmlElement(name = "version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty(value = "issuer")
    @XmlElement(name = "issuer")
    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @JsonProperty(value = "aat_profiles_supported")
    @XmlElement(name = "aat_profiles_supported")
    public String[] getAatProfilesSupported() {
        return aatProfilesSupported;
    }

    public void setAatProfilesSupported(String[] oauthTokenProfilesSupported) {
        this.aatProfilesSupported = oauthTokenProfilesSupported;
    }

    @JsonProperty(value = "rpt_profiles_supported")
    @XmlElement(name = "rpt_profiles_supported")
    public String[] getRptProfilesSupported() {
        return rptProfilesSupported;
    }

    public void setRptProfilesSupported(String[] umaTokenProfilesSupported) {
        this.rptProfilesSupported = umaTokenProfilesSupported;
    }

    @JsonProperty(value = "aat_grant_types_supported")
    @XmlElement(name = "aat_grant_types_supported")
    public String[] getAatGrantTypesSupported() {
        return aatGrantTypesSupported;
    }

    public void setAatGrantTypesSupported(String[] p_aatGrantTypesSupported) {
        this.aatGrantTypesSupported = p_aatGrantTypesSupported;
    }


    @JsonProperty(value = "pat_grant_types_supported")
    @XmlElement(name = "pat_grant_types_supported")
    public String[] getPatGrantTypesSupported() {
        return patGrantTypesSupported;
    }

    public void setPatGrantTypesSupported(String[] p_patGrantTypesSupported) {
        this.patGrantTypesSupported = p_patGrantTypesSupported;
    }

    @JsonProperty(value = "claim_profiles_supported")
    @XmlElement(name = "claim_profiles_supported")
    public String[] getClaimProfilesSupported() {
        return claimProfilesSupported;
    }

    public void setClaimProfilesSupported(String[] claimProfilesSupported) {
        this.claimProfilesSupported = claimProfilesSupported;
    }

    @JsonProperty(value = "dynamic_client_endpoint")
    @XmlElement(name = "dynamic_client_endpoint")
    public String getDynamicClientEndpoint() {
        return dynamicClientEndpoint;
    }

    public void setDynamicClientEndpoint(String dynamicClientEndpoint) {
        this.dynamicClientEndpoint = dynamicClientEndpoint;
    }

    @JsonProperty(value = "token_endpoint")
    @XmlElement(name = "token_endpoint")
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @JsonProperty(value = "user_endpoint")
    @XmlElement(name = "user_endpoint")
    public String getUserEndpoint() {
        return userEndpoint;
    }

    public void setUserEndpoint(String userEndpoint) {
        this.userEndpoint = userEndpoint;
    }

    @JsonProperty(value = "resource_set_registration_endpoint")
    @XmlElement(name = "resource_set_registration_endpoint")
    public String getResourceSetRegistrationEndpoint() {
        return resourceSetRegistrationEndpoint;
    }

    public void setResourceSetRegistrationEndpoint(String resourceSetRegistrationEndpoint) {
        this.resourceSetRegistrationEndpoint = resourceSetRegistrationEndpoint;
    }

    @JsonProperty(value = "introspection_endpoint")
    @XmlElement(name = "introspection_endpoint")
    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String rptStatusEndpoint) {
        this.introspectionEndpoint = rptStatusEndpoint;
    }

    @JsonProperty(value = "permission_registration_endpoint")
    @XmlElement(name = "permission_registration_endpoint")
    public String getPermissionRegistrationEndpoint() {
        return permissionRegistrationEndpoint;
    }

    public void setPermissionRegistrationEndpoint(String permissionRegistrationEndpoint) {
        this.permissionRegistrationEndpoint = permissionRegistrationEndpoint;
    }

    @JsonProperty(value = "rpt_endpoint")
    @XmlElement(name = "rpt_endpoint")
    public String getRptEndpoint() {
        return rptEndpoint;
    }

    public void setRptEndpoint(String rptEndpoint) {
        this.rptEndpoint = rptEndpoint;
    }

    @JsonProperty(value = "authorization_request_endpoint")
    @XmlElement(name = "authorization_request_endpoint")
    public String getAuthorizationRequestEndpoint() {
        return authorizationRequestEndpoint;
    }

    public void setAuthorizationRequestEndpoint(String permissionRequestEndpoint) {
        this.authorizationRequestEndpoint = permissionRequestEndpoint;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MetadataConfiguration [version=");
        builder.append(version);
        builder.append(", issuer=");
        builder.append(issuer);
        builder.append(", patProfilesSupported=");
        builder.append(Arrays.toString(patProfilesSupported));
        builder.append(", aatProfilesSupported=");
        builder.append(Arrays.toString(aatProfilesSupported));
        builder.append(", rptProfilesSupported=");
        builder.append(Arrays.toString(rptProfilesSupported));
        builder.append(", patGrantTypesSupported=");
        builder.append(Arrays.toString(patGrantTypesSupported));
        builder.append(", aatGrantTypesSupported=");
        builder.append(Arrays.toString(aatGrantTypesSupported));
        builder.append(", claimProfilesSupported=");
        builder.append(Arrays.toString(claimProfilesSupported));
        builder.append(", tokenEndpoint=");
        builder.append(tokenEndpoint);
        builder.append(", userEndpoint=");
        builder.append(userEndpoint);
        builder.append(", introspection_endpoint=");
        builder.append(introspectionEndpoint);
        builder.append(", resourceSetRegistrationEndpoint=");
        builder.append(resourceSetRegistrationEndpoint);
        builder.append(", permissionRegistrationEndpoint=");
        builder.append(permissionRegistrationEndpoint);
        builder.append(", rptEndpoint=");
        builder.append(rptEndpoint);
        builder.append(", authorization_request_endpoint=");
        builder.append(authorizationRequestEndpoint);
        builder.append("]");
        return builder.toString();
    }

}
