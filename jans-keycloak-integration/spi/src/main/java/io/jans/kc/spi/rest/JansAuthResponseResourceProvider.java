package io.jans.kc.spi.rest;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;

import io.jans.kc.spi.auth.SessionAttributes;

import java.util.Map;
import java.util.HashMap;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

public class JansAuthResponseResourceProvider implements RealmResourceProvider {

    private static final Logger log = Logger.getLogger(JansAuthResponseResourceProvider.class);

    private static final String ACTION_URI_TPL_PARAM = "actionuri";
    private static final String ERR_MSG_TPL_PARAM = "authError";

    private static final String JANS_AUTH_RESPONSE_ERR_FTL ="jans-auth-response-error.ftl";
    private static final String JANS_AUTH_RESPONSE_COMPLETE_FTL = "jans-auth-response-complete.ftl";

    private static final String ERR_MSG_INVALID_REALM = "jans.error-invalid-realm";
    private static final String ERR_MSG_MISSING_DATA  = "jans.error-missing-data";

    private KeycloakSession session;

    public JansAuthResponseResourceProvider(KeycloakSession session) {

        this.session = session;
    }

    @Override
    public Object getResource() {

        return this;
    }

    @Override
    public void close() {
        //nothing to do for now 
    }

    @GET
    @NoCache
    @Produces(MediaType.TEXT_HTML)
    @Path("/auth-complete")
    public Response completeAuthentication(@QueryParam("code") String code, 
        @QueryParam("scope") String scope,
        @QueryParam("state") String state) {
        
       RealmModel realm = getAuthenticationRealm();
       if(!stateIsAssociatedToRealm(realm, state)) {
           log.infov("Realm {0} is not associated to authz response and state {1}",realm.getName(),state);
           return createErrorResponse(ERR_MSG_INVALID_REALM);
       }

       if(!realmHasActionUri(realm)) {
          log.infov("Realm {0} has no action uri set to complete authentication",realm.getName());
          return createErrorResponse(ERR_MSG_MISSING_DATA);
       }
       saveAuthResultInRealm(realm, code, state);
       return createFinalizeAuthResponse(realm.getAttribute(SessionAttributes.KC_ACTION_URI));
    }

    private final RealmModel getAuthenticationRealm() {

        return session.getContext().getRealm();
    }

    private final boolean stateIsAssociatedToRealm(RealmModel realm , String state) {

        String expectedstate = realm.getAttribute(SessionAttributes.JANS_OIDC_STATE);

        return state.equals(expectedstate);
    }

    private final boolean realmHasActionUri(RealmModel realm) {

        String actionuri = realm.getAttribute(SessionAttributes.KC_ACTION_URI);
        return (actionuri != null);
    }

    private final void saveAuthResultInRealm(RealmModel realm, String code, String sessionState) {

        realm.setAttribute(SessionAttributes.JANS_OIDC_CODE,code);
        realm.setAttribute(SessionAttributes.JANS_SESSION_STATE,sessionState);
    }

    private final Response createResponseWithForm(String formtemplate,Map<String,String> attributes) {

        LoginFormsProvider lfp = session.getProvider(LoginFormsProvider.class);

        if(attributes != null && !attributes.isEmpty()) {
            for(Map.Entry<String,String> attrEntry: attributes.entrySet()) {
                lfp.setAttribute(attrEntry.getKey(),attrEntry.getValue());
            }
        }
        return lfp.createForm(formtemplate);
    }

    private final Response createErrorResponse(String errmsgid) {

        Map<String,String> attributes = new HashMap<>();
        attributes.put(ERR_MSG_TPL_PARAM,errmsgid);
        return createResponseWithForm(JANS_AUTH_RESPONSE_ERR_FTL,attributes);
    }

    private final Response createFinalizeAuthResponse(String actionuri) {

        Map<String,String> attributes  = new HashMap<>();
        attributes.put(ACTION_URI_TPL_PARAM,actionuri);
        return createResponseWithForm(JANS_AUTH_RESPONSE_COMPLETE_FTL, attributes);
    }
}
