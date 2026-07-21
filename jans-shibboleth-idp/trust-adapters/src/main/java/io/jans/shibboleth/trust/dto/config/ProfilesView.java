package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A trust relationship's profile configurations, keyed by profile; only the requested profiles are present.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfilesView {

    @JsonProperty("shibboleth_sso")
    private ShibbolethSsoProfileConfigurationView shibbolethSso;

    @JsonProperty("saml2_sso")
    private Saml2SsoProfileConfigurationView saml2Sso;

    @JsonProperty("saml2_artifact_resolution")
    private Saml2ArtifactResolutionProfileConfigurationView saml2ArtifactResolution;

    @JsonProperty("saml2_attribute_query")
    private Saml2AttributeQueryProfileConfigurationView saml2AttributeQuery;

    @JsonProperty("saml2_ecp")
    private Saml2EcpProfileConfigurationView saml2Ecp;

    @JsonProperty("saml2_logout")
    private Saml2LogoutProfileConfigurationView saml2Logout;

    public ProfilesView() {
    }

    public ShibbolethSsoProfileConfigurationView getShibbolethSso() {

        return shibbolethSso;
    }

    public void setShibbolethSso(ShibbolethSsoProfileConfigurationView shibbolethSso) {

        this.shibbolethSso = shibbolethSso;
    }

    public Saml2SsoProfileConfigurationView getSaml2Sso() {

        return saml2Sso;
    }

    public void setSaml2Sso(Saml2SsoProfileConfigurationView saml2Sso) {

        this.saml2Sso = saml2Sso;
    }

    public Saml2ArtifactResolutionProfileConfigurationView getSaml2ArtifactResolution() {

        return saml2ArtifactResolution;
    }

    public void setSaml2ArtifactResolution(Saml2ArtifactResolutionProfileConfigurationView saml2ArtifactResolution) {

        this.saml2ArtifactResolution = saml2ArtifactResolution;
    }

    public Saml2AttributeQueryProfileConfigurationView getSaml2AttributeQuery() {

        return saml2AttributeQuery;
    }

    public void setSaml2AttributeQuery(Saml2AttributeQueryProfileConfigurationView saml2AttributeQuery) {

        this.saml2AttributeQuery = saml2AttributeQuery;
    }

    public Saml2EcpProfileConfigurationView getSaml2Ecp() {

        return saml2Ecp;
    }

    public void setSaml2Ecp(Saml2EcpProfileConfigurationView saml2Ecp) {

        this.saml2Ecp = saml2Ecp;
    }

    public Saml2LogoutProfileConfigurationView getSaml2Logout() {

        return saml2Logout;
    }

    public void setSaml2Logout(Saml2LogoutProfileConfigurationView saml2Logout) {

        this.saml2Logout = saml2Logout;
    }
}
