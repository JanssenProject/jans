package org.xdi.oxauth.uma.authorization;

import org.apache.commons.lang.StringUtils;
import org.gluu.jsf2.service.FacesService;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtClaims;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.service.UserService;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;
import org.xdi.oxauth.uma.service.RedirectParameters;
import org.xdi.oxauth.uma.service.UmaPctService;
import org.xdi.oxauth.uma.service.UmaPermissionService;
import org.xdi.oxauth.uma.service.UmaSessionService;
import org.xdi.oxauth.uma.ws.rs.UmaMetadataWS;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz on 06/18/2017.
 */
public class UmaGatherContext extends ExternalScriptContext {

    private final UmaSessionService sessionService;
    private final UmaPermissionService permissionService;
    private final UmaPctService pctService;
    private final UserService userService;
    private final FacesService facesService;

    private final Map<String, SimpleCustomProperty> configurationAttributes;
    private final AppConfiguration appConfiguration;
    private final SessionState session;
    private final RedirectParameters redirectUserParameters = new RedirectParameters();
    private final UmaPCT pct;
    private final JwtClaims claims;
    private final Map<String, String> pageClaims;

    public UmaGatherContext(Map<String, SimpleCustomProperty> configurationAttributes, HttpServletRequest httpRequest, SessionState session, UmaSessionService sessionService,
                            UmaPermissionService permissionService, UmaPctService pctService, Map<String, String> pageClaims,
                            UserService userService, FacesService facesService, AppConfiguration appConfiguration) {
        super(httpRequest);
        this.configurationAttributes = configurationAttributes;
        this.session = session;
        this.sessionService = sessionService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.pctService = pctService;
        this.facesService = facesService;
        this.pct = pctService.getByCode(sessionService.getPct(session));
        this.claims = pct.getClaims();
        this.pageClaims = pageClaims;
        this.appConfiguration = appConfiguration;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
        return configurationAttributes;
    }

    public User getUser(String... returnAttributes) {
        String userDn = getUserDn();
        if (StringUtils.isNotBlank(userDn)) {
            return userService.getUserByDn(userDn, returnAttributes);
        }
        return null;
    }

    public String getUserDn() {
        SessionState connectSession = sessionService.getConnectSession(httpRequest);
        if (connectSession != null) {
            return connectSession.getUserDn();
        }
        return null;
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
        claims.setClaimObject(claimName, claimValue);
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

    public void redirect(String url) {
        facesService.redirectToExternalURL(url);
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
