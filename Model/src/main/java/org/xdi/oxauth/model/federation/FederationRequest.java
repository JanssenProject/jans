/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.federation;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/09/2012
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthFederationRequest"})
public class FederationRequest {

    public static enum Type {
        RP("rp"), OP("op");

        private final String m_value;

        private Type(String p_value) {
            m_value = p_value;
        }

        public String getValue() {
            return m_value;
        }

        public static Type fromValue(String p_str) {
            if (StringUtils.isNotBlank(p_str)) {
                for (Type t : values()) {
                    if (t.m_value.equalsIgnoreCase(p_str)) {
                        return t;
                    }
                }
            }
            return null;
        }
    }

    @LdapDN
    private String dn;
    @LdapAttribute(name = "inum")
    private String id;
    @LdapAttribute(name = "oxAuthFederationOpId")
    private String federationId;
    @LdapAttribute(name = "oxAuthFederationRequestType")
    private String entityType;
    @LdapAttribute(name = "displayName")
    private String displayName;
    @LdapAttribute(name = "oxAuthFederationOpId")
    private String opId;
    @LdapAttribute(name = "oxAuthFederationOpDomain")
    private String domain;
    @LdapAttribute(name = "oxAuthRedirectURI")
    private List<String> redirectUri;

    private Type type;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String p_displayName) {
        displayName = p_displayName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String p_domain) {
        domain = p_domain;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String p_entityType) {
        entityType = p_entityType;
    }

    public String getFederationId() {
        return federationId;
    }

    public void setFederationId(String p_federationId) {
        federationId = p_federationId;
    }

    public String getOpId() {
        return opId;
    }

    public void setOpId(String p_opId) {
        opId = p_opId;
    }

    public List<String> getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(List<String> p_redirectUri) {
        redirectUri = p_redirectUri;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type p_type) {
        type = p_type;
    }
}
