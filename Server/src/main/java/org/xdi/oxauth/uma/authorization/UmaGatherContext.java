package org.xdi.oxauth.uma.authorization;

import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;
import org.xdi.oxauth.uma.service.RedirectParameters;
import org.xdi.oxauth.uma.service.UmaSessionService;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz on 06/18/2017.
 */
public class UmaGatherContext extends ExternalScriptContext {

    private final SessionState session;
    private final UmaSessionService sessionService;
    private final RedirectParameters redirectUserParameters = new RedirectParameters();

    public UmaGatherContext(HttpServletRequest httpRequest, SessionState session, UmaSessionService sessionService) {
        super(httpRequest);
        this.session = session;
        this.sessionService = sessionService;
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

}
