/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.authorization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.uma.ClaimToken;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version 0.9 February 12, 2015
 */

public class AuthorizationContext extends ExternalScriptContext {

    private final UmaRPT m_rpt;
    private final ResourceSetPermission m_permission;
    private final IAuthorizationGrant m_grant;
    private final Map<String, List<String>> m_claims;
    private NeedInfoAuthenticationContext needInfoAuthenticationContext;
    private NeedInfoRequestingPartyClaims needInfoRequestingPartyClaims;

    public AuthorizationContext(UmaRPT p_rpt, ResourceSetPermission p_permission, IAuthorizationGrant p_grant,
                                HttpServletRequest p_httpRequest, List<ClaimToken> claims) {
    	super(p_httpRequest);

    	m_rpt = p_rpt;
        m_permission = p_permission;
        m_grant = p_grant;
        m_claims = new HashMap<String, List<String>>();
        if (claims != null) {
            for (ClaimToken claim : claims) {
                List<String> strings = m_claims.get(claim.getFormat());
                if (strings == null) {
                    strings = new ArrayList<String>();
                }
                strings.add(claim.getToken());
                m_claims.put(claim.getFormat(), strings);
            }
        }
    }

    public List<String> getRequestClaim(String p_claimName) {
        if (StringUtils.isNotBlank(p_claimName) && m_claims != null) {
            final List<String> value = m_claims.get(p_claimName);
            if (value != null) {
                return Collections.unmodifiableList(value);
            }
        }
        return Collections.emptyList();
    }

    public IAuthorizationGrant getGrant() {
        return m_grant;
    }

    public String getAcrs() {
        return m_grant.getAcrValues();
    }

    public String getClientClaim(String p_claimName) {
        return getEntryAttributeValue(getGrant().getClientDn(), p_claimName);
    }

    public String getUserClaim(String p_claimName) {
        GluuAttribute gluuAttribute = AttributeService.instance().getByClaimName(p_claimName);

        if (gluuAttribute != null) {
            String ldapClaimName = gluuAttribute.getName();
            return getEntryAttributeValue(getGrant().getUserDn(), ldapClaimName);
        }

        return null;
    }

    public String getUserClaimByLdapName(String p_ldapName) {
        return getEntryAttributeValue(getGrant().getUserDn(), p_ldapName);
    }

    public UmaRPT getRpt() {
        return m_rpt;
    }

    public ResourceSetPermission getPermission() {
        return m_permission;
    }

    public NeedInfoAuthenticationContext getNeedInfoAuthenticationContext() {
        return needInfoAuthenticationContext;
    }

    public void setNeedInfoAuthenticationContext(NeedInfoAuthenticationContext needInfoAuthenticationContext) {
        this.needInfoAuthenticationContext = needInfoAuthenticationContext;
    }

    public NeedInfoRequestingPartyClaims getNeedInfoRequestingPartyClaims() {
        return needInfoRequestingPartyClaims;
    }

    public void setNeedInfoRequestingPartyClaims(NeedInfoRequestingPartyClaims needInfoRequestingPartyClaims) {
        this.needInfoRequestingPartyClaims = needInfoRequestingPartyClaims;
    }
}
