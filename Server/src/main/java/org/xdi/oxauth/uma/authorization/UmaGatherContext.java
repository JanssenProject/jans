package org.xdi.oxauth.uma.authorization;

import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;
import org.xdi.oxauth.uma.service.UmaSessionService;

import javax.servlet.http.HttpServletRequest;

/**
 * @author yuriyz on 06/18/2017.
 */
public class UmaGatherContext extends ExternalScriptContext {

    private final SessionState session;
    private final UmaSessionService sessionService;

    public UmaGatherContext(HttpServletRequest httpRequest, SessionState session, UmaSessionService sessionService) {
        super(httpRequest);
        this.session = session;
        this.sessionService = sessionService;
    }
}
