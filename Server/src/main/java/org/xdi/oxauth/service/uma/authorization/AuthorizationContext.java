/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.authorization;

import org.apache.commons.lang.StringUtils;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.uma.ClaimToken;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version 0.9 February 12, 2015
 */

public class AuthorizationContext extends ExternalScriptContext {

    private final UmaRPT rpt;
    private final ResourceSetPermission permission;
    private final IAuthorizationGrant grant;
    private final Map<String, List<String>> claims;
    private NeedInfoAuthenticationContext needInfoAuthenticationContext;
    private NeedInfoRequestingPartyClaims needInfoRequestingPartyClaims;

    private AttributeService attributeService;

    public AuthorizationContext(AttributeService attributeService, UmaRPT p_rpt, ResourceSetPermission p_permission, IAuthorizationGrant p_grant,
                                HttpServletRequest p_httpRequest, List<ClaimToken> claims) {
    	super(p_httpRequest);

    	this.attributeService = attributeService;
        this.rpt = p_rpt;
        this.permission = p_permission;
        this.grant = p_grant;
        this.claims = new HashMap<String, List<String>>();
        if (claims != null) {
            for (ClaimToken claim : claims) {
                List<String> strings = this.claims.get(claim.getFormat());
                if (strings == null) {
                    strings = new ArrayList<String>();
                }
                strings.add(claim.getToken());
                this.claims.put(claim.getFormat(), strings);
            }
        }
    }

    public List<String> getRequestClaim(String p_claimName) {
        if (StringUtils.isNotBlank(p_claimName) && claims != null) {
            final List<String> value = claims.get(p_claimName);
            if (value != null) {
                return Collections.unmodifiableList(value);
            }
        }
        return Collections.emptyList();
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

    public UmaRPT getRpt() {
        return rpt;
    }

    public ResourceSetPermission getPermission() {
        return permission;
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
