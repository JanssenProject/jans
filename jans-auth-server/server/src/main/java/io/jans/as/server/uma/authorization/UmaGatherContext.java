/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.authorization;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.as.server.uma.service.RedirectParameters;
import io.jans.as.server.uma.service.UmaPctService;
import io.jans.as.server.uma.service.UmaPermissionService;
import io.jans.as.server.uma.service.UmaSessionService;
import io.jans.as.server.uma.ws.rs.UmaMetadataWS;
import io.jans.model.SimpleCustomProperty;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz
 * @version August 9, 2017
 */
public class UmaGatherContext extends ExternalScriptContext {

    private final UmaSessionService sessionService;
    private final UmaPermissionService permissionService;
    private final UmaPctService pctService;

    private final Map<String, SimpleCustomProperty> configurationAttributes;
    private final AppConfiguration appConfiguration;
    private final SessionId session;
    private final RedirectParameters redirectUserParameters = new RedirectParameters();
    private final UmaPCT pct;
    private final JwtClaims claims;
    private final Map<String, String> pageClaims;
    private String redirectToExternalUrl = null;

    public UmaGatherContext(Map<String, SimpleCustomProperty> configurationAttributes, HttpServletRequest httpRequest, SessionId session, UmaSessionService sessionService,
                            UmaPermissionService permissionService, UmaPctService pctService, Map<String, String> pageClaims, AppConfiguration appConfiguration) {
        super(httpRequest);
        this.configurationAttributes = configurationAttributes;
        this.session = session;
        this.sessionService = sessionService;
        this.permissionService = permissionService;
        this.pctService = pctService;
        this.pct = pctService.getByCode(sessionService.getPct(session));
        this.claims = pct.getClaims();
        this.pageClaims = pageClaims;
        this.appConfiguration = appConfiguration;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
        return configurationAttributes;
    }

    public User getUser() {
        return sessionService.getUser(httpRequest);
    }

    public String getUserDn() {
        return sessionService.getUserDn(httpRequest);
    }


    public Client getClient() {
        return sessionService.getClient(session);
    }

    public Map<String, String> getConnectSessionAttributes() {
        SessionId connectSession = sessionService.getConnectSession(httpRequest);
        if (connectSession != null) {
            return new HashMap<>(connectSession.getSessionAttributes());
        }
        return new HashMap<>();
    }

    public boolean isAuthenticated() {
        return getUser() != null;
    }

    public Map<String, String> getPageClaims() {
        return pageClaims;
    }

    public Map<String, String[]> getRequestParameters() {
        return httpRequest.getParameterMap();
    }

    public int getStep() {
        return sessionService.getStep(session);
    }

    public void setStep(int step) {
        sessionService.setStep(step, session);
    }

    public void addSessionAttribute(String key, String value) {
        session.getSessionAttributes().put(key, value);
    }

    public void removeSessionAttribute(String key) {
        session.getSessionAttributes().remove(key);
    }

    public Map<String, String> getSessionAttributes() {
        return session.getSessionAttributes();
    }

    public void addRedirectUserParam(String paramName, String paramValue) {
        redirectUserParameters.add(paramName, paramValue);
    }

    public void removeRedirectUserParameter(String paramName) {
        redirectUserParameters.remove(paramName);
    }

    public RedirectParameters getRedirectUserParameters() {
        return redirectUserParameters;
    }

    public Map<String, Set<String>> getRedirectUserParametersMap() {
        return redirectUserParameters.map();
    }

    public List<UmaPermission> getPermissions() {
        return permissionService.getPermissionsByTicket(sessionService.getTicket(session));
    }

    public JwtClaims getClaims() {
        return claims;
    }

    public Object getClaim(String claimName) {
        return claims.getClaim(claimName);
    }

    public void putClaim(String claimName, Object claimValue) {
        claims.setClaimObject(claimName, claimValue, true);
    }

    public void removeClaim(String claimName) {
        claims.removeClaim(claimName);
    }

    public boolean hasClaim(String claimName) {
        return getClaim(claimName) != null;
    }

    /**
     * Must not take any parameters
     */
    public void persist() {
        try {
            pct.setClaims(claims);
        } catch (InvalidJwtException e) {
            getLog().error("Failed to persist claims", e);
        }

        sessionService.persist(session);
        pctService.merge(pct);
    }

    public void redirectToExternalUrl(String url) {
        redirectToExternalUrl = url;
    }

    public String getRedirectToExternalUrl() {
        return redirectToExternalUrl;
    }

    public String getAuthorizationEndpoint() {
        return appConfiguration.getAuthorizationEndpoint();
    }

    public String getIssuer() {
        return appConfiguration.getIssuer();
    }

    public String getBaseEndpoint() {
        return appConfiguration.getBaseEndpoint();
    }

    public String getClaimsGatheringEndpoint() {
        return getBaseEndpoint() + UmaMetadataWS.UMA_CLAIMS_GATHERING_PATH;
    }
}
