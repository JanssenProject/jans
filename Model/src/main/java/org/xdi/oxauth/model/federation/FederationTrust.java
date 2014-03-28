package org.xdi.oxauth.model.federation;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/09/2012
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthFederationTrust"})
public class FederationTrust {
    @LdapDN
    private String dn;
    @LdapAttribute(name = "inum")
    private String id;
    @LdapAttribute(name = "displayName")
    private String displayName;
    @LdapAttribute(name = "oxAuthFederationId")
    private String federationId;
    @LdapAttribute(name = "oxAuthFederationMetadataURI")
    private String federationMetadataUri;
    @LdapAttribute(name = "oxAuthReleasedScope")
    private List<String> scopes;
    @LdapAttribute(name = "oxAuthRedirectURI")
    private List<String> redirectUris;
    @LdapAttribute(name = "oxAuthFederationTrustStatus")
    private String trustStatus;
    @LdapAttribute(name = "oxAuthSkipAuthorization")
    private Boolean skipAuthorization;

    private FederationTrustStatus status;

    public String getTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(String p_trustStatus) {
        trustStatus = p_trustStatus;
    }

    public FederationTrustStatus getStatus() {
        if (status == null && StringUtils.isNotBlank(trustStatus)) {
            status = FederationTrustStatus.fromValue(trustStatus);
        }
        return status;
    }

    public void setStatus(FederationTrustStatus p_status) {
        status = p_status;
        if (p_status != null) {
            trustStatus = p_status.getValue();
        }
    }

    public Boolean getSkipAuthorization() {
        return skipAuthorization;
    }

    public void setSkipAuthorization(Boolean p_skipAuthorization) {
        skipAuthorization = p_skipAuthorization;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String p_displayName) {
        displayName = p_displayName;
    }

    public String getFederationId() {
        return federationId;
    }

    public void setFederationId(String p_federationId) {
        federationId = p_federationId;
    }

    public String getFederationMetadataUri() {
        return federationMetadataUri;
    }

    public void setFederationMetadataUri(String p_federationMetadataUri) {
        federationMetadataUri = p_federationMetadataUri;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> p_redirectUris) {
        redirectUris = p_redirectUris;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> p_scopes) {
        scopes = p_scopes;
    }
}
