package org.xdi.oxauth.service.token;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.common.util.StringHelper;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;

/**
 * Token specific service methods
 *
 * @author Yuriy Movchan Date: 10/03/2012
 */
@Scope(ScopeType.STATELESS)
@Name("tokenService")
@AutoCreate
public class TokenService {

//	@Logger
//    private Log log;
    @In
    private AuthorizationGrantList authorizationGrantList;

	public String getTokenFromAuthorizationParameter(String authorizationParameter) {
        final String prefix = "Bearer ";
        if (StringHelper.isNotEmpty(authorizationParameter) && authorizationParameter.startsWith(prefix)) {
        	return authorizationParameter.substring(prefix.length());
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

    public String getClientDn(String p_authorization) {
        final AuthorizationGrant grant = getAuthorizationGrant(p_authorization);
        if (grant != null) {
            return grant.getClientDn();
        }
        return "";
    }

}
