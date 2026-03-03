package io.jans.idp.authn.action;

import io.jans.idp.authn.context.JansAuthenticationContext;
import io.jans.idp.authn.impl.JansAuthenticationService;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class InitializeJansAuthenticationAction extends AbstractAuthenticationAction {

    private static final Logger LOG = LoggerFactory.getLogger(InitializeJansAuthenticationAction.class);

    private JansAuthenticationService authenticationService;

    public InitializeJansAuthenticationAction() {
        super();
    }

    public void setAuthenticationService(@Nonnull JansAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doExecute(@Nonnull ProfileRequestContext profileRequestContext,
                             @Nonnull AuthenticationContext authenticationContext) {
        
        LOG.debug("Initializing Janssen authentication");
        
        JansAuthenticationContext jansContext = authenticationContext.getSubcontext(
            JansAuthenticationContext.class, true);
        
        String relyingPartyId = null;
        if (profileRequestContext.getInboundMessageContext() != null) {
            Object inboundMessage = profileRequestContext.getInboundMessageContext().getMessage();
            if (inboundMessage != null) {
                relyingPartyId = extractRelyingPartyId(inboundMessage);
            }
        }
        
        jansContext.setRelayingPartyId(relyingPartyId);
        
        String authorizationUrl = authenticationService.buildAuthorizationUrl(jansContext);
        jansContext.setExternalProviderUri(authorizationUrl);
        
        LOG.debug("Initialized Janssen authentication context for RP: {}", relyingPartyId);
    }

    private String extractRelyingPartyId(Object inboundMessage) {
        try {
            if (inboundMessage instanceof org.opensaml.saml.saml2.core.AuthnRequest) {
                org.opensaml.saml.saml2.core.AuthnRequest authnRequest = 
                    (org.opensaml.saml.saml2.core.AuthnRequest) inboundMessage;
                if (authnRequest.getIssuer() != null) {
                    return authnRequest.getIssuer().getValue();
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not extract relying party ID from inbound message", e);
        }
        return null;
    }
}
