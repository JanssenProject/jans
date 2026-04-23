package io.jans.shibboleth.model;

import io.jans.shibboleth.model.config.profiles.*;

public class Profiles {

    private final ShibbolethSsoProfileConfiguration shibbolethSso;
    private final Saml2AttributeQueryProfileConfiguration saml2AttributeQuery;
    private final Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolution;
    private final Saml2EcpProfileConfiguration saml2Ecp;
    private final Saml2SsoProfileConfiguration saml2Sso;
    private final Saml2LogoutProfileConfiguration saml2Logout;

    private Profiles(
        ShibbolethSsoProfileConfiguration shibbolethSso, Saml2AttributeQueryProfileConfiguration saml2AttributeQuery,
        Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolution, Saml2EcpProfileConfiguration saml2Ecp, 
        Saml2SsoProfileConfiguration saml2Sso, Saml2LogoutProfileConfiguration saml2Logout ) {

        this.shibbolethSso = shibbolethSso != null ? shibbolethSso : ShibbolethSsoProfileConfiguration.defaultConfiguration();
        this.saml2AttributeQuery = saml2AttributeQuery != null ? saml2AttributeQuery : Saml2AttributeQueryProfileConfiguration.defaultConfiguration();
        this.saml2ArtifactResolution = saml2ArtifactResolution != null ? saml2ArtifactResolution : Saml2ArtifactResolutionProfileConfiguration.defaultConfiguration();
        this.saml2Ecp = saml2Ecp != null ? saml2Ecp : Saml2EcpProfileConfiguration.defaultConfiguration();
        this.saml2Sso = saml2Sso != null ? saml2Sso : Saml2SsoProfileConfiguration.defaultConfiguration();
        this.saml2Logout = saml2Logout != null ? saml2Logout : Saml2LogoutProfileConfiguration.defaultConfiguration();
    }

    public static Profiles allDefaults() {

        return new Profiles(null,null,null,null,null,null);
    }

    public ShibbolethSsoProfileConfiguration getShibbolethSso() {

        return shibbolethSso;
    }

    public Saml2AttributeQueryProfileConfiguration getSaml2AttributeQuery() {

        return saml2AttributeQuery;
    }

    public Saml2ArtifactResolutionProfileConfiguration getSaml2ArtifactResolution() {

        return saml2ArtifactResolution;
    }

    public Saml2EcpProfileConfiguration getSaml2Ecp() {

        return saml2Ecp;
    }

    public Saml2SsoProfileConfiguration getSaml2Sso() {

        return saml2Sso;
    }

    public Saml2LogoutProfileConfiguration getSaml2Logout() {

        return saml2Logout;
    }
}