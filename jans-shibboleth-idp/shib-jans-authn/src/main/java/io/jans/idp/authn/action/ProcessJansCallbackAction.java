package io.jans.idp.authn.action;

import io.jans.idp.authn.context.JansAuthenticationContext;
import io.jans.idp.authn.impl.JansAuthenticationService;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import jakarta.servlet.http.HttpServletRequest;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class ProcessJansCallbackAction extends AbstractAuthenticationAction {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessJansCallbackAction.class);

    private JansAuthenticationService authenticationService;

    public ProcessJansCallbackAction() {
        super();
    }

    public void setAuthenticationService(@Nonnull JansAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doExecute(@Nonnull ProfileRequestContext profileRequestContext,
                             @Nonnull AuthenticationContext authenticationContext) {
        
        LOG.debug("Processing Janssen authentication callback");
        
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            LOG.error("No HTTP request available");
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");
        
        if (error != null) {
            String errorDescription = request.getParameter("error_description");
            LOG.error("OAuth error: {} - {}", error, errorDescription);
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.AUTHN_EXCEPTION);
            return;
        }
        
        if (code == null || state == null) {
            LOG.error("Missing code or state parameter in callback");
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        JansAuthenticationContext jansContext = authenticationContext.getSubcontext(
            JansAuthenticationContext.class);
        
        if (jansContext == null) {
            LOG.error("No Janssen authentication context found");
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.AUTHN_EXCEPTION);
            return;
        }
        
        boolean success = authenticationService.processAuthorizationResponse(jansContext, code, state);
        
        if (success && jansContext.isAuthenticated()) {
            String userPrincipal = jansContext.getUserPrincipal();
            LOG.info("Authentication successful for user: {}", userPrincipal);
            
            Subject subject = new Subject();
            subject.getPrincipals().add(new UsernamePrincipal(userPrincipal));
            
            buildAuthenticationResult(profileRequestContext, authenticationContext);
            authenticationContext.getAuthenticationResult().setSubject(subject);
            
        } else {
            LOG.error("Authentication failed: {}", jansContext.getErrorMessage());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.AUTHN_EXCEPTION);
        }
    }
}
