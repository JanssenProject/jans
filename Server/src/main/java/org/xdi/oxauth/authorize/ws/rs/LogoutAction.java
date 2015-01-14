/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.authorize.ws.rs;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesManager;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.log.Log;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.session.EndSessionRequestParam;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas Blum Date: 03.13.2012
 */
@Name("logoutAction")
@Scope(ScopeType.EVENT)
public class LogoutAction {

    @Logger
    private Log log;

    @In
    private FacesMessages facesMessages;

    @In
    private AuthorizationGrantList authorizationGrantList;

    @In
    private ExternalAuthenticationService externalAuthenticationService;

    private String idTokenHint;
    private String postLogoutRedirectUri;


    public String getIdTokenHint() {
		return idTokenHint;
	}

	public void setIdTokenHint(String idTokenHint) {
		this.idTokenHint = idTokenHint;
	}

	public String getPostLogoutRedirectUri() {
		return postLogoutRedirectUri;
	}

	public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
		this.postLogoutRedirectUri = postLogoutRedirectUri;
	}

    public void redirect() {
        boolean externalLogoutResult = processExternalAuthenticatorLogOut();
        if (!externalLogoutResult) {
        	logoutFailed();
        	return;
        }

        StringBuilder sb = new StringBuilder();

        // Required parameters
        if(idTokenHint!=null && !idTokenHint.isEmpty()){
            sb.append(EndSessionRequestParam.ID_TOKEN_HINT + "=").append(idTokenHint);
        }

        if (postLogoutRedirectUri != null && !postLogoutRedirectUri.isEmpty()) {
            sb.append("&"+EndSessionRequestParam.POST_LOGOUT_REDIRECT_URI+"=").append(postLogoutRedirectUri);
        }
        
        FacesManager.instance().redirectToExternalURL("seam/resource/restv1/oxauth/end_session?" + sb.toString());
    }

	private boolean processExternalAuthenticatorLogOut() {
		AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
		if (authorizationGrant == null) {
			return false;
		}

		String authMode = authorizationGrant.getAuthMode();
		boolean isExternalAuthenticatorLogoutPresent = StringHelper.isNotEmpty(authMode);
		if (isExternalAuthenticatorLogoutPresent) {
			log.debug("Attemptinmg to execute logout method of '{0}' external authenticator.", authMode);

			CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService.getCustomScriptConfigurationByName(authMode);
			if (customScriptConfiguration == null) {
				log.error("Failed to get ExternalAuthenticatorConfiguration. auth_mode: {0}", authMode);
				return false;
			} else {
				boolean externalLogoutResult = externalAuthenticationService.executeExternalAuthenticatorLogout(
						customScriptConfiguration, null);
				log.debug("Logout result for {0}. result: {1}", authorizationGrant.getUser().getUserId(), authMode, externalLogoutResult);

				return externalLogoutResult;
			}
		} else {
			return true;
		}
	}

	public void logoutFailed() {
		facesMessages.add(Severity.ERROR, "Failed to process logout");
		FacesManager.instance().redirect("/error.xhtml");
	}

}