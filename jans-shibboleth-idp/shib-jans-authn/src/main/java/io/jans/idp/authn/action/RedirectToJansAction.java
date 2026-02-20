package io.jans.idp.authn.action;

import io.jans.idp.authn.context.JansAuthenticationContext;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class RedirectToJansAction extends AbstractAuthenticationAction {

    private static final Logger LOG = LoggerFactory.getLogger(RedirectToJansAction.class);

    public RedirectToJansAction() {
        super();
    }

    @Override
    protected void doExecute(@Nonnull ProfileRequestContext profileRequestContext,
                             @Nonnull AuthenticationContext authenticationContext) {
        
        LOG.debug("Preparing redirect to Janssen Auth Server");
        
        JansAuthenticationContext jansContext = authenticationContext.getSubcontext(
            JansAuthenticationContext.class);
        
        if (jansContext == null) {
            LOG.error("No Janssen authentication context found");
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.AUTHN_EXCEPTION);
            return;
        }
        
        String redirectUrl = jansContext.getExternalProviderUri();
        
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            LOG.error("No redirect URL available in context");
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.AUTHN_EXCEPTION);
            return;
        }
        
        LOG.debug("Redirect URL prepared: {}", redirectUrl);
    }

    public String getRedirectUrl(@Nonnull AuthenticationContext authenticationContext) {
        JansAuthenticationContext jansContext = authenticationContext.getSubcontext(
            JansAuthenticationContext.class);
        
        if (jansContext != null) {
            return jansContext.getExternalProviderUri();
        }
        
        return null;
    }
}
