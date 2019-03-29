/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service.token;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.AuthorizationGrantList;
import org.gluu.util.StringHelper;

/**
 * Token specific service methods
 *
 * @author Yuriy Movchan Date: 10/03/2012
 */
@Stateless
@Named
public class TokenService {

    @Inject
    private AuthorizationGrantList authorizationGrantList;

	public String getTokenFromAuthorizationParameter(String authorizationParameter) {
        if (StringHelper.isNotEmpty(authorizationParameter)) {
            if (authorizationParameter.startsWith("Bearer ")) {
                return authorizationParameter.substring("Bearer ".length());
            }
            if (authorizationParameter.startsWith("Basic ")) {
                return authorizationParameter.substring("Basic ".length());
            }
        }
        return null;
	}

    public AuthorizationGrant getAuthorizationGrant(String p_authorization) {
        final String token = getTokenFromAuthorizationParameter(p_authorization);
        if (StringUtils.isNotBlank(token)) {
    	    return authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        }
        return null;
    }

    public AuthorizationGrant getAuthorizationGrantByPrefix(String authorization, String prefix) {
        if (StringUtils.startsWithIgnoreCase(authorization, prefix)) {
            return authorizationGrantList.getAuthorizationGrantByAccessToken(authorization.substring(prefix.length()));
        }
        return null;
    }

    public String getClientDn(String p_authorization) {
        final AuthorizationGrant grant = getAuthorizationGrant(p_authorization);
        if (grant != null) {
            return grant.getClientDn();
        }
        return "";
    }

}
