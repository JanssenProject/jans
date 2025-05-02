package io.jans.as.server.auth;

import com.google.common.collect.Maps;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.*;
import io.jans.as.server.service.external.ExternalCreateUserService;
import io.jans.jsf2.service.FacesService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * @author Yuriy Z
 */
@RequestScoped
@Named
public class CreateUserAction {

    private static final String FORM_ID = "createForm";
    private static final String CREATE_BUTTON_REF = FORM_ID + ":createButton";

    // OAuth 2.0 request parameters
    private String scope;
    private String responseType;
    private String clientId;
    private String redirectUri;
    private String state;

    // OpenID Connect request parameters
    private String responseMode;
    private String nonce;
    private String display;
    private String prompt;
    private Integer maxAge;
    private String uiLocales;
    private String idTokenHint;
    private String loginHint;
    private String acrValues;
    private String amrValues;
    private String request;
    private String requestUri;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String claims;
    private String authReqId;
    private String bindingMessage;
    private String sessionId;
    private String allowedScope;

    private String displayName;
    private String email;
    private String uid;
    private String password;

    @Inject
    private Logger log;

    @Inject
    private ExternalCreateUserService externalCreateUserService;

    @Inject
    private ErrorHandlerService errorHandlerService;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private FacesService facesService;

    @Inject
    private UserService userService;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private CookieService cookieService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private AppConfiguration appConfiguration;

    @PostConstruct
    public void prepare() {
        log.trace("Preparing CreateUserAction");

        ExecutionContext executionContext = ExecutionContext.of(externalContext);

        final boolean isOk = externalCreateUserService.externalPrepare(executionContext);
        if (!isOk) {
            errorHandlerService.handleError("createUser.forbiddenByScript", AuthorizeErrorResponseType.ACCESS_DENIED, "Forbidden by createUser script.");
        }
    }

    public void createUser() {
        try {
            if (BooleanUtils.isTrue(appConfiguration.getDisablePromptCreate())) {
                log.debug("Skipped user creation. config disablePromptCreate=true");
                return;
            }

            log.debug("Creating user ...");

            ExecutionContext executionContext = ExecutionContext.of(externalContext);
            User user = prepareUserObject(executionContext);

            final boolean isOk = externalCreateUserService.externalCreate(executionContext);
            if (!isOk) {
                log.debug("createUser is forbidded by create() method of external script.");
                return;
            }

            user = userService.addUser(user, true);
            log.debug("User {} is created successfully.", user.getUserId());

            String uri = buildAuthorizationUrl();
            log.trace("RedirectTo: {}", uri);
            facesService.redirectToExternalURL(uri);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private User prepareUserObject(ExecutionContext executionContext) {
        User user = new User();
        executionContext.setUser(user);

        final User fetchedUser = userService.getUser(uid);
        if (fetchedUser != null) {
            log.debug("User with uid {} already exists.", uid);
            user = fetchedUser;
        }

        user.setAttribute("displayName", displayName, false);
        user.setAttribute("mail", email, false);
        user.setAttribute("uid", uid, false);
        user.setAttribute("userPassword", password, false);

        log.debug("Prepared user - uid: {}, email: {}, displayName: {}", uid, email, displayName);
        return user;
    }

    private String buildAuthorizationUrl() throws UnsupportedEncodingException {
        final ExecutionContext executionContext = ExecutionContext.of(externalContext);
        final String url = externalCreateUserService.externalBuildPostAuthorizeUrl(executionContext);
        if (StringUtils.isNotBlank(url)) {
            log.debug("Authorization Url is returned from external script, url: {}", url);
            return url;
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();

        final Map<String, String> filteredParameters = getFilteredParameters();
        filteredParameters.remove("password");
        filteredParameters.remove("email");

        log.debug("client_id {}, response_type {}, scope {}, acr_values {}", clientId, responseType, scope, acrValues);

        filteredParameters.putIfAbsent("client_id", clientId);
        filteredParameters.putIfAbsent("response_type", responseType);
        filteredParameters.putIfAbsent("response_mode", responseMode);
        filteredParameters.putIfAbsent("scope", scope);
        filteredParameters.putIfAbsent("redirect_uri", redirectUri);
        filteredParameters.putIfAbsent("nonce", nonce);
        filteredParameters.putIfAbsent("acr_values", acrValues);
        filteredParameters.putIfAbsent("state", state);
        filteredParameters.putIfAbsent("request", request);
        filteredParameters.putIfAbsent("request_uri", requestUri);
        filteredParameters.putIfAbsent("display", display);
        filteredParameters.putIfAbsent("max_age", maxAge != null ? maxAge.toString() : null);
        filteredParameters.putIfAbsent("ui_locales", uiLocales);
        filteredParameters.putIfAbsent("id_token_hint", idTokenHint);
        filteredParameters.putIfAbsent("login_hint", loginHint);
        filteredParameters.putIfAbsent("amr_values", amrValues);
        filteredParameters.putIfAbsent("code_challenge", codeChallenge);
        filteredParameters.putIfAbsent("code_challenge_method", codeChallengeMethod);
        filteredParameters.putIfAbsent("claims", claims);
        filteredParameters.putIfAbsent("auth_req_id", authReqId);
        filteredParameters.putIfAbsent("binding_message", bindingMessage);
        filteredParameters.putIfAbsent("session_id", sessionId);
        filteredParameters.putIfAbsent("allowed_scope", allowedScope);

        return httpRequest.getContextPath() + "/restv1/authorize?" + requestParameterService.parametersAsString(filteredParameters);
    }

    public SessionId getSession() {
        return getSession(null);
    }

    public SessionId getSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            sessionId = cookieService.getSessionIdFromCookie();
            if (StringUtils.isBlank(sessionId)) {
                return null;
            }
        }

        SessionId dbSessionId = sessionIdService.getSessionId(sessionId);
        log.debug("Found session {}, dbSession: {}", sessionId, dbSessionId);
        return dbSessionId;
    }

    private Map<String, String> getFilteredParameters() {
        final Map<String, String> parameterMap = externalContext.getRequestParameterMap();
        final Map<String, String> filtered = Maps.newHashMap();
        final String formIdWithColon = FORM_ID + ":";

        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            final String key = entry.getKey();
            if (key.equals("jakarta.faces.ViewState") || key.equals(FORM_ID) || key.contains(CREATE_BUTTON_REF)) {
                continue;
            }
            if (key.startsWith(formIdWithColon)) {
                filtered.put(StringUtils.removeStart(key, formIdWithColon), entry.getValue());
                continue;
            }
            filtered.put(StringUtils.removeStart(key, formIdWithColon), entry.getValue());
        }
        return filtered;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public String getUiLocales() {
        return uiLocales;
    }

    public void setUiLocales(String uiLocales) {
        this.uiLocales = uiLocales;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getAmrValues() {
        return amrValues;
    }

    public void setAmrValues(String amrValues) {
        this.amrValues = amrValues;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getClaims() {
        return claims;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAllowedScope() {
        return allowedScope;
    }

    public void setAllowedScope(String allowedScope) {
        this.allowedScope = allowedScope;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
