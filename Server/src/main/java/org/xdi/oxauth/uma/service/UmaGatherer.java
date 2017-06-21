package org.xdi.oxauth.uma.service;

import com.ocpsoft.pretty.faces.util.StringUtils;
import org.gluu.jsf2.service.FacesService;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.i18n.LanguageBean;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.uma.authorization.UmaGatherContext;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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
    @Inject
    private UmaPermissionService umaPermissionService;
    @Inject
    private UmaPctService umaPctService;

    public boolean gather() {
        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
        final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();
        final SessionState session = umaSessionService.getSession(httpRequest, httpResponse);

        CustomScriptConfiguration script = umaSessionService.getScript(session);
        UmaGatherContext context = new UmaGatherContext(httpRequest, session, umaSessionService, umaPermissionService, umaPctService);

        int step = umaSessionService.getStep(session);
        int stepsCount = external.getStepsCount(script, context);

        boolean gatheredResult = external.gather(script, step, context);
        context.persist();

        if (step == stepsCount) {
            onSuccess(session, context);
        }

        if (gatheredResult) {
            return true;
        }
        return false;
    }

    private void onSuccess(SessionState session, UmaGatherContext context) {
        List<UmaPermission> permissions = context.getPermissions();
        String newTicket = umaPermissionService.changeTicket(permissions, permissions.get(0).getAttributes());

        facesService.redirectToExternalURL(constructRedirectUri(session, context, newTicket));
    }

    private String constructRedirectUri(SessionState session, UmaGatherContext context, String newTicket) {
        String claimsRedirectUri = umaSessionService.getClaimsRedirectUri(session);

        addQueryParameters(claimsRedirectUri, context.getRedirectUserParameters().buildQueryString().trim());
        addQueryParameter(claimsRedirectUri, "state", umaSessionService.getState(session));
        addQueryParameter(claimsRedirectUri, "ticket", newTicket);
        return claimsRedirectUri;
    }

    public static String addQueryParameters(String url, String parameters) {
        if (StringUtils.isNotBlank(parameters)) {
            if (url.contains("?")) {
                url += "&" + parameters;
            } else {
                url += "?" + parameters;
            }
        }
        return url;
    }

    public static String addQueryParameter(String url, String paramName, String paramValue) {
        if (StringUtils.isNotBlank(paramValue)) {
            if (url.contains("?")) {
                url += "&" + paramName + "=" + paramValue;
            } else {
                url += "?" + paramName + "=" + paramValue;
            }
        }
        return url;
    }
}
