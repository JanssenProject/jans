package io.jans.as.server.util;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.authorize.ws.rs.ConsentGatheringSessionService;
import io.jans.as.server.service.*;
import io.jans.as.common.model.session.SessionId;
import io.jans.service.cdi.util.CdiUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * An utilitarian class for developers writing Agama Consent flows
 */
@ApplicationScoped
public class AgamaConsentUtil {
    
    @Inject
    private ConsentGatheringSessionService cgss;
    
    @Inject
    private AuthorizeService as;
    
    @Inject
    private SessionIdService sis;

    public Map<String, String> getSessionAttributes() {
        return getSessionId().getSessionAttributes();
    }
    
    public Client getClient() {
         return cgss.getClient(getSessionId());
    }
    
    public List<Scope> getScopes() {
        String scope = getSessionAttributes().get("scope");
        return Optional.ofNullable(scope).map(as::getScopes).orElse(Collections.emptyList()); 
    }
    
    public User getUser() {
        return sis.getUser(getSessionId());
    }
    
    private SessionId getSessionId() {
        return sis.getSessionId(getServletRequest());
    }
    
    private HttpServletRequest getServletRequest() {
        return CdiUtil.bean(HttpServletRequest.class);
    }
    
}
