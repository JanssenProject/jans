/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.authorization;

import org.apache.commons.lang.StringUtils;
import org.xdi.ldap.model.CustomEntry;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version 0.9 February 12, 2015
 */

public class UmaAuthorizationContext extends ExternalScriptContext {

    private final UmaRPT rpt;
    private final UmaPermission permission;
    private final IAuthorizationGrant grant;
    private final Claims claims;
    private NeedInfoAuthenticationContext needInfoAuthenticationContext;
    private NeedInfoError needInfoRequestingPartyClaims;

    private AttributeService attributeService;

    public UmaAuthorizationContext(AttributeService attributeService, UmaRPT p_rpt, UmaPermission p_permission, IAuthorizationGrant p_grant,
                                   HttpServletRequest p_httpRequest, Claims claims) {
    	super(p_httpRequest);

    	this.attributeService = attributeService;
        this.rpt = p_rpt;
        this.permission = p_permission;
        this.grant = p_grant;
        this.claims = claims;
    }

    public Object getRequestClaim(String claimName) {
        if (StringUtils.isNotBlank(claimName) && claims != null) {
            return claims.get(claimName);
        }
        return null;
    }

    public IAuthorizationGrant getGrant() {
        return grant;
    }

    public String getAcrs() {
        return grant.getAcrValues();
    }

    public String getClientClaim(String p_claimName) {
        return getEntryAttributeValue(getGrant().getClientDn(), p_claimName);
    }

    public String getUserClaim(String p_claimName) {
        GluuAttribute gluuAttribute = attributeService.getByClaimName(p_claimName);

        if (gluuAttribute != null) {
            String ldapClaimName = gluuAttribute.getName();
            return getEntryAttributeValue(getGrant().getUserDn(), ldapClaimName);
        }

        return null;
    }

    public String getUserClaimByLdapName(String p_ldapName) {
        return getEntryAttributeValue(getGrant().getUserDn(), p_ldapName);
    }

    public CustomEntry getUserClaimEntryByLdapName(String ldapName) {
        return getEntryByDn(getGrant().getUserDn(), ldapName);
    }

    public CustomEntry getClientClaimEntry(String ldapName) {
        return getEntryByDn(getGrant().getClientDn(), ldapName);
    }

    public UmaRPT getRpt() {
        return rpt;
    }

    public UmaPermission getPermission() {
        return permission;
    }

    public NeedInfoAuthenticationContext getNeedInfoAuthenticationContext() {
        return needInfoAuthenticationContext;
    }

    public void setNeedInfoAuthenticationContext(NeedInfoAuthenticationContext needInfoAuthenticationContext) {
        this.needInfoAuthenticationContext = needInfoAuthenticationContext;
    }

    public NeedInfoError getNeedInfoRequestingPartyClaims() {
        return needInfoRequestingPartyClaims;
    }

    public void setNeedInfoRequestingPartyClaims(NeedInfoError needInfoRequestingPartyClaims) {
        this.needInfoRequestingPartyClaims = needInfoRequestingPartyClaims;
    }
}
