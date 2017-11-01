/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.service.external.context;

import org.gluu.jsf2.service.FacesService;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.oxauth.authorize.ws.rs.ConsentGatheringSessionService;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtClaims;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.service.UserService;
import org.xdi.oxauth.uma.service.RedirectParameters;
import org.xdi.oxauth.uma.service.UmaPctService;
import org.xdi.oxauth.uma.service.UmaPermissionService;
import org.xdi.oxauth.uma.service.UmaSessionService;
import org.xdi.oxauth.uma.ws.rs.UmaMetadataWS;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Yuriy Movchan Date: 10/30/2017
 */
public class ConsentGatheringContext extends ExternalScriptContext {

    private final ConsentGatheringSessionService sessionService;
    private final UserService userService;
    private final FacesService facesService;

    private final Map<String, SimpleCustomProperty> configurationAttributes;
    private final AppConfiguration appConfiguration;
    private final SessionId session;
    private final RedirectParameters redirectUserParameters = new RedirectParameters();
    private final JwtClaims claims;
    private final Map<String, String> pageClaims;
    private String redirectToExternalUrl = null;

    public ConsentGatheringContext(Map<String, SimpleCustomProperty> configurationAttributes, HttpServletRequest httpRequest, SessionId session, ConsentGatheringSessionService sessionService,
                            Map<String, String> pageClaims, JwtClaims claims,
                            UserService userService, FacesService facesService, AppConfiguration appConfiguration) {
        super(httpRequest);
        this.configurationAttributes = configurationAttributes;
        this.session = session;
        this.sessionService = sessionService;
        this.userService = userService;
        this.facesService = facesService;
        this.claims = claims;
        this.pageClaims = pageClaims;
        this.appConfiguration = appConfiguration;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
        return configurationAttributes;
    }

    public User getUser(String... returnAttributes) {
        return sessionService.getUser(httpRequest, returnAttributes);
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
            return new HashMap<String, String>(connectSession.getSessionAttributes());
        }
        return new HashMap<String, String>();
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
//        try {
//            pct.setClaims(claims);
//        } catch (InvalidJwtException e) {
//            getLog().error("Failed to persist claims", e);
//        }
//
        sessionService.persist(session);
//        pctService.merge(pct);
    }

    public void redirectToExternalUrl(String url) {
        redirectToExternalUrl = url;
    }

    public String getRedirectToExternalUrl() {
        return redirectToExternalUrl;
    }

}
