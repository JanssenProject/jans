package org.xdi.oxauth.uma.authorization;

import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.jwt.JwtClaims;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;
import org.xdi.oxauth.uma.service.RedirectParameters;
import org.xdi.oxauth.uma.service.UmaPctService;
import org.xdi.oxauth.uma.service.UmaPermissionService;
import org.xdi.oxauth.uma.service.UmaSessionService;

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

    private final SessionState session;
    private final RedirectParameters redirectUserParameters = new RedirectParameters();
    private final UmaPCT pct;

    public UmaGatherContext(HttpServletRequest httpRequest, SessionState session, UmaSessionService sessionService,
                            UmaPermissionService permissionService, UmaPctService pctService) {
        super(httpRequest);
        this.session = session;
        this.sessionService = sessionService;
        this.permissionService = permissionService;
        this.pctService = pctService;
        this.pct = pctService.getByCode(sessionService.getPct(session));
    }

    public int getStep() {
        return sessionService.getStep(session);
    }

    public void setStep(int step) {
        sessionService.setStep(step, session);
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
        return pct.getClaims();
    }

    /**
     * Must not take any parameters
     */
    public void persist() {
        sessionService.persist(session);
        pctService.persist(pct);
    }
}
