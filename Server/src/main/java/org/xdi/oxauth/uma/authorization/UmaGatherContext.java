package org.xdi.oxauth.uma.authorization;

import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtClaims;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;
import org.xdi.oxauth.uma.service.RedirectParameters;
import org.xdi.oxauth.uma.service.UmaPctService;
import org.xdi.oxauth.uma.service.UmaPermissionService;
import org.xdi.oxauth.uma.service.UmaSessionService;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author yuriyz on 06/18/2017.
 */
public class UmaGatherContext extends ExternalScriptContext {

    private final UmaSessionService sessionService;
    private final UmaPermissionService permissionService;
    private final UmaPctService pctService;

    private final SessionState session;
    private final RedirectParameters redirectUserParameters = new RedirectParameters();
    private final UmaPCT pct;
    private final JwtClaims claims;

    public UmaGatherContext(HttpServletRequest httpRequest, SessionState session, UmaSessionService sessionService,
                            UmaPermissionService permissionService, UmaPctService pctService) {
        super(httpRequest);
        this.session = session;
        this.sessionService = sessionService;
        this.permissionService = permissionService;
        this.pctService = pctService;
        this.pct = pctService.getByCode(sessionService.getPct(session));
        this.claims = pct.getClaims();
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
        pctService.persist(pct);
    }
}
