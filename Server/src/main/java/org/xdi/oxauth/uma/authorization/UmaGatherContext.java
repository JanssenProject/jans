package org.xdi.oxauth.uma.authorization;

import org.xdi.oxauth.service.external.context.ExternalScriptContext;

import javax.servlet.http.HttpServletRequest;

/**
 * @author yuriyz on 06/18/2017.
 */
public class UmaGatherContext extends ExternalScriptContext {

    public UmaGatherContext(HttpServletRequest httpRequest) {
        super(httpRequest);
    }
}
