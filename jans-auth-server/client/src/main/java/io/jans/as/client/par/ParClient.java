package io.jans.as.client.par;

import io.jans.as.client.BaseClient;
import io.jans.as.client.ClientAuthnEnabler;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import org.apache.log4j.Logger;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ParClient extends BaseClient<ParRequest, ParResponse> {

    private static final Logger LOG = Logger.getLogger(ParClient.class);

    /**
     * Constructs an par client by providing a REST url where the authorize service is located.
     *
     * @param url The REST Service location.
     */
    public ParClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public ParResponse exec() {
        try {
            return exec_();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return null;
    }

    private ParResponse exec_() throws Exception {
        // Prepare request parameters
        initClient();

        final String responseTypesAsString = getRequest().getAuthorizationRequest().getResponseTypesAsString();
        final String scopesAsString = getRequest().getAuthorizationRequest().getScopesAsString();
        final String promptsAsString = getRequest().getAuthorizationRequest().getPromptsAsString();
        final String uiLocalesAsString = getRequest().getAuthorizationRequest().getUiLocalesAsString();
        final String claimLocalesAsString = getRequest().getAuthorizationRequest().getClaimsLocalesAsString();
        final String acrValuesAsString = getRequest().getAuthorizationRequest().getAcrValuesAsString();
        final String claimsAsString = getRequest().getAuthorizationRequest().getClaimsAsString();

        addReqParam(AuthorizeRequestParam.RESPONSE_TYPE, responseTypesAsString);
        addReqParam(AuthorizeRequestParam.CLIENT_ID, getRequest().getAuthorizationRequest().getClientId());
        addReqParam(AuthorizeRequestParam.SCOPE, scopesAsString);
        addReqParam(AuthorizeRequestParam.REDIRECT_URI, getRequest().getAuthorizationRequest().getRedirectUri());
        addReqParam(AuthorizeRequestParam.STATE, getRequest().getAuthorizationRequest().getState());
        addReqParam(AuthorizeRequestParam.NBF, getRequest().getNbf() != null ? getRequest().getNbf().toString() : null);

        addReqParam(AuthorizeRequestParam.NONCE, getRequest().getAuthorizationRequest().getNonce());
        addReqParam(AuthorizeRequestParam.DISPLAY, getRequest().getAuthorizationRequest().getDisplay());
        addReqParam(AuthorizeRequestParam.PROMPT, promptsAsString);
        if (getRequest().getAuthorizationRequest().getMaxAge() != null) {
            addReqParam(AuthorizeRequestParam.MAX_AGE, getRequest().getAuthorizationRequest().getMaxAge().toString());
        }
        addReqParam(AuthorizeRequestParam.UI_LOCALES, uiLocalesAsString);
        addReqParam(AuthorizeRequestParam.CLAIMS_LOCALES, claimLocalesAsString);
        addReqParam(AuthorizeRequestParam.ID_TOKEN_HINT, getRequest().getAuthorizationRequest().getIdTokenHint());
        addReqParam(AuthorizeRequestParam.LOGIN_HINT, getRequest().getAuthorizationRequest().getLoginHint());
        addReqParam(AuthorizeRequestParam.ACR_VALUES, acrValuesAsString);
        addReqParam(AuthorizeRequestParam.CLAIMS, claimsAsString);
        addReqParam(AuthorizeRequestParam.REGISTRATION, getRequest().getAuthorizationRequest().getRegistration());
        addReqParam(AuthorizeRequestParam.REQUEST, getRequest().getAuthorizationRequest().getRequest());
        addReqParam(AuthorizeRequestParam.REQUEST_URI, getRequest().getAuthorizationRequest().getRequestUri());
        addReqParam(AuthorizeRequestParam.ACCESS_TOKEN, getRequest().getAuthorizationRequest().getAccessToken());
        addReqParam(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS, getRequest().getAuthorizationRequest().getCustomResponseHeadersAsString());

        // PKCE
        addReqParam(AuthorizeRequestParam.CODE_CHALLENGE, getRequest().getAuthorizationRequest().getCodeChallenge());
        addReqParam(AuthorizeRequestParam.CODE_CHALLENGE_METHOD, getRequest().getAuthorizationRequest().getCodeChallengeMethod());

        addReqParam(AuthorizeRequestParam.SESSION_ID, getRequest().getAuthorizationRequest().getSessionId());

        // Custom params
        for (String key : request.getCustomParameters().keySet()) {
            addReqParam(key, request.getCustomParameters().get(key));
        }

        Builder clientRequest = webTarget.request();

        new ClientAuthnEnabler(clientRequest, requestForm).exec(request);

        clientRequest.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        clientResponse = clientRequest.buildPost(Entity.form(requestForm)).invoke();

        setResponse(new ParResponse(clientResponse));

        return getResponse();
    }
}
