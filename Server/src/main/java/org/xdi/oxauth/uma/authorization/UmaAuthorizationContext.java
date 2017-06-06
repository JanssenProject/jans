/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.authorization;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaResource;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version 0.9 February 12, 2015
 */

public class UmaAuthorizationContext extends ExternalScriptContext {

    private final Claims claims;
    private final List<String> scopes;
    private final List<UmaResource> resources;

    private AttributeService attributeService;

    public UmaAuthorizationContext(AttributeService attributeService, List<String> scopes,
                                   List<UmaResource> resources, Claims claims, HttpServletRequest httpRequest) {
    	super(httpRequest);

    	this.attributeService = attributeService;
        this.scopes = scopes;
        this.resources = resources;
        this.claims = claims;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public List<UmaResource> getResources() {
        return resources;
    }

    public Object getRequestClaim(String claimName) {
        if (StringUtils.isNotBlank(claimName) && claims != null) {
            return claims.get(claimName);
        }
        return null;
    }

//    public String getClientClaim(String p_claimName) {
//        return getEntryAttributeValue(getGrant().getClientDn(), p_claimName);
//    }
//
//    public String getUserClaim(String p_claimName) {
//        GluuAttribute gluuAttribute = attributeService.getByClaimName(p_claimName);
//
//        if (gluuAttribute != null) {
//            String ldapClaimName = gluuAttribute.getName();
//            return getEntryAttributeValue(getGrant().getUserDn(), ldapClaimName);
//        }
//
//        return null;
//    }
//
//    public String getUserClaimByLdapName(String p_ldapName) {
//        return getEntryAttributeValue(getGrant().getUserDn(), p_ldapName);
//    }
//
//    public CustomEntry getUserClaimEntryByLdapName(String ldapName) {
//        return getEntryByDn(getGrant().getUserDn(), ldapName);
//    }
//
//    public CustomEntry getClientClaimEntry(String ldapName) {
//        return getEntryByDn(getGrant().getClientDn(), ldapName);
//    }

    public UmaPermission getPermission() {
        return permission;
    }
}
