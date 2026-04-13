package io.jans.shibboleth.model;

import io.jans.shibboleth.model.profiles.*;

public class Profiles {

    private Saml2Sso saml2sso;
    private Saml2Ecp saml2ecp;
    private Saml2AttributeQuery saml2attributeQuery;
    private Saml2ArtifactResolution saml2artifactResolution;
    private ShibbolethSso shibbolethSso;
    private Saml2Logout saml2logout;

    public boolean isActive(ProfileType type) {

        switch(type) {

            case SHIBBOLETH_SSO:
                return shibbolethSso.isActive();
                break;
            case SAML2_ATTRIBUTE_QUERY:
                return saml2attributeQuery.isActive();
                break;
            case SAML2_ATTRIBUTE_RESOLUTION:
                return saml2artifactResolution.isActive();
                break;
            case SAML2_ECP:
                return saml2ecp.isActive();
                break;
            case SAML2_SSO:
                return saml2sso.isActive();
                break;
            case SAML2_LOGOUT:
                return saml2logout.isActive();
                break;
            default:
                break;
        }
        
        return false;
    }
}