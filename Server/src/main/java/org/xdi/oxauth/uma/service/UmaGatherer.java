package org.xdi.oxauth.uma.service;

import org.gluu.jsf2.service.FacesService;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.i18n.LanguageBean;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.uma.authorization.UmaGatherContext;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author yuriyz on 06/20/2017.
 */
@RequestScoped
@Named(value = "gatherer")
public class UmaGatherer {

    @Inject
    private ExternalUmaClaimsGatheringService external;
    @Inject
    private AppConfiguration appConfiguration;
    @Inject
    private FacesContext facesContext;
    @Inject
    private ExternalContext externalContext;
    @Inject
    private FacesService facesService;
    @Inject
    private LanguageBean languageBean;
    @Inject
    private UmaSessionService umaSessionService;

    public boolean gather() {
        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
        final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();
        final SessionState session = umaSessionService.getSession(httpRequest, httpResponse);

        CustomScriptConfiguration script = umaSessionService.getScript(session);
        UmaGatherContext context = new UmaGatherContext(httpRequest, session, umaSessionService);
        int step = umaSessionService.getStep(session);

        if (external.gather(script, step, context)) {
            return true;
        }
        return false;
    }
}
