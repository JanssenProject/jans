package io.jans.kc.spi.auth;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.security.SecureRandom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import io.jans.kc.spi.ProviderIDs;
import io.jans.kc.oidc.OIDCAuthRequest;
import io.jans.kc.oidc.OIDCMetaError;
import io.jans.kc.oidc.OIDCService;
import io.jans.kc.oidc.OIDCTokenError;
import io.jans.kc.oidc.OIDCTokenRequest;
import io.jans.kc.oidc.OIDCTokenRequestError;
import io.jans.kc.oidc.OIDCTokenResponse;
import io.jans.kc.oidc.OIDCUserInfoError;
import io.jans.kc.oidc.OIDCUserInfoRequestError;
import io.jans.kc.oidc.OIDCUserInfoResponse;

public class JansAuthenticator implements Authenticator {
    
    private static final Logger log = Logger.getLogger(JansAuthenticator.class);

    private static final String JANS_AUTH_REDIRECT_FORM_FTL  = "jans-auth-redirect.ftl";
    private static final String JANS_AUTH_ERROR_FTL = "jans-auth-error.ftl";
    
    private static final String OPENID_CODE_RESPONSE = "code";
    private static final String OPENID_SCOPE = "openid";
    private static final String USERNAME_SCOPE ="user_name";
    private static final String EMAIL_SCOPE = "email";
    private static final String JANS_LOGIN_URL_ATTRIBUTE = "jansLoginUrl";
    private static final String OPENID_AUTH_PARAMS_ATTRIBUTE = "openIdAuthParams";

    private static final String URI_PATH_TO_REST_SERVICE = "realms/{realm}/{providerid}/auth-complete";
    
    
    private OIDCService oidcService;

    public JansAuthenticator(OIDCService oidcService) {

        this.oidcService = oidcService;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        Configuration config = extractAndValidateConfiguration(context);
        if(config == null) {
            context.failure(AuthenticationFlowError.INTERNAL_ERROR,onConfigurationError(context));
            return;
        }

        Response response = null;
        try {
            URI redirecturi = createRedirectUri(context);

            if(redirecturi == null) {
                log.error("Invalid redirect URI");
                response = context.form().createForm(JANS_AUTH_ERROR_FTL);
                context.failure(AuthenticationFlowError.INTERNAL_ERROR,response);
                return;
            }
            URI actionuri = createActionUrl(context);

            String state = generateOIDCState();
            String nonce = generateOIDCNonce();

            OIDCAuthRequest oidcauthrequest = createAuthnRequest(config, state, nonce,redirecturi.toString());
        
            URI loginurl = oidcService.createAuthorizationUrl(config.normalizedIssuerUrl(), oidcauthrequest);
            URI loginurlnoparams = UriBuilder.fromUri(loginurl.toString()).replaceQuery(null).build();
            
            response = context
                .form()
                .setActionUri(actionuri)
                .setAttribute(JANS_LOGIN_URL_ATTRIBUTE,loginurlnoparams.toString())
                .setAttribute(OPENID_AUTH_PARAMS_ATTRIBUTE,parseQueryParameters(loginurl.getQuery()))
                .createForm(JANS_AUTH_REDIRECT_FORM_FTL);

            saveRealmStringData(context, SessionAttributes.JANS_OIDC_NONCE, nonce);
            saveRealmStringData(context, SessionAttributes.KC_ACTION_URI,actionuri.toString());
            saveRealmStringData(context,SessionAttributes.JANS_OIDC_STATE,state);

            context.challenge(response);
        }catch(OIDCMetaError e) {
            log.errorv(e,"OIDC Error obtaining the authorization url");
            response = context.form().createForm(JANS_AUTH_ERROR_FTL);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR,response);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {

        Configuration config = extractAndValidateConfiguration(context);
        if(config == null) {
            context.failure(AuthenticationFlowError.INTERNAL_ERROR,onMissingAuthenticationCode(context));
            return;
        }

        String openidCode = getOpenIdCode(context);
        if(openidCode == null) {
            log.errorv("Missing authentication code during response processing");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR,onMissingAuthenticationCode(context));
            return;
        }

        OIDCTokenRequest tokenrequest = createTokenRequest(config, openidCode, createRedirectUri(context));
        try {
            OIDCTokenResponse tokenresponse = oidcService.requestTokens(config.normalizedIssuerUrl(), tokenrequest);
            if(!tokenresponse.indicatesSuccess()) {
                OIDCTokenError error = tokenresponse.error();
                log.errorv("Error processing token {0}. ({1}) {2}",error.code(),error.description());
                context.failure(AuthenticationFlowError.INTERNAL_ERROR,onTokenRetrievalError(context));
                return;
            }

            OIDCUserInfoResponse userinforesponse = oidcService.requestUserInfo(config.normalizedIssuerUrl(),tokenresponse.accessToken());
            if(!userinforesponse.indicatesSuccess()) {
                OIDCUserInfoError error = userinforesponse.error();
                log.errorv("Error getting userinfo for authenticated user. ({0}) {1}",error.code(),error.description());
                context.failure(AuthenticationFlowError.INTERNAL_ERROR,onUserInfoRetrievalError(context));
                return;
            }

            UserModel user = findUserByNameOrEmail(context,userinforesponse.username(),userinforesponse.email());
            if(user == null) {
                log.errorv("User with username/email {0} / {1} not found",userinforesponse.username(),userinforesponse.email());
                context.failure(AuthenticationFlowError.UNKNOWN_USER);
                return;
            }
            log.debugv("User {0} authenticated",user.getUsername());
            context.setUser(user);
            context.success();
        }catch(OIDCTokenRequestError e) {
            log.debugv(e,"Unable to retrieve token information");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR,onTokenRetrievalError(context));
        }catch(OIDCUserInfoRequestError e) {
            log.debugv(e,"Unable to retrieve user information");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR,onUserInfoRetrievalError(context));
        }
    }

    @Override
    public boolean requiresUser() {

        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {

        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel model, UserModel user) {
        //for now no required actions to specify
    }

    @Override
    public List<RequiredActionFactory> getRequiredActions(KeycloakSession session) {

        return new ArrayList<>();
    }

    @Override
    public void close() {

        // nothing to do for now when then authenticator is shutdown
    }

    private Configuration extractAndValidateConfiguration(AuthenticationFlowContext  context) {
        
        Configuration config = pluginConfigurationFromContext(context);

        if(config == null) {
            log.debugv("Plugin probably not configured. Check the Janssen Auth plugin in the authentication flow");
            return null;
        }

        ValidationResult validationresult = config.validate();
        if(validationresult.hasErrors()) {
            for(String err : validationresult.getErrors()) {
                log.errorv("Invalid plugin configuration {0}",err);
            }
            return null;
        }
        return config;
    }

    private URI createRedirectUri(AuthenticationFlowContext context) {

        try {
            String realmname = context.getRealm().getName();
            return UriBuilder.fromUri(context.getSession().getContext().getUri().getBaseUri())
                      .path(URI_PATH_TO_REST_SERVICE)
                      .build(realmname,ProviderIDs.JANS_AUTH_RESPONSE_REST_PROVIDER);
        }catch(IllegalArgumentException e) {
            log.warnv(e,"Could not create redirect URIs");
            return null;
        }
    }

    private UserModel findUserByNameOrEmail(AuthenticationFlowContext context, String username,String email) {
        
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(),context.getRealm(),username);
        if(user == null) {
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(),context.getRealm(),email);
        }
        return user;
    }


    private Map<String,String> parseQueryParameters(String params) {

        Map<String,String> ret = new HashMap<>();
        if(params == null) {
            return ret;
        }

        String [] parampairs = params.split("&");
        for(String pair : parampairs) {
            String [] kv = pair.split("=");
            if(kv.length == 1) {
                ret.put(kv[0].trim(),"");
            }else {
                try {
                    ret.put(kv[0].trim(),
                        URLDecoder.decode(kv[1].trim(),"UTF-8"));
                }catch(UnsupportedEncodingException ue) {
                    log.debugv(ue,"Failed to decode query parameter data {0}",pair);
                }
            }
        }

        return ret;
    }

    private Configuration pluginConfigurationFromContext(AuthenticationFlowContext context) {
        
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if(config == null || config.getConfig() == null) {
            return null;
        }

        String serverUrl = config.getConfig().get(JansAuthenticatorConfigProp.SERVER_URL.getName());
        String clientId  = config.getConfig().get(JansAuthenticatorConfigProp.CLIENT_ID.getName());
        String clientSecret = config.getConfig().get(JansAuthenticatorConfigProp.CLIENT_SECRET.getName());
        String issuer = config.getConfig().get(JansAuthenticatorConfigProp.ISSUER.getName());
        String extraScopes = config.getConfig().get(JansAuthenticatorConfigProp.EXTRA_SCOPES.getName());
        List<String> parsedExtraScopes = new ArrayList<>();
        if(extraScopes != null) {
            String [] tokens = extraScopes.split(",");
            for(String token : tokens) {
                parsedExtraScopes.add(token.trim());
            }
        }

        return new Configuration(serverUrl,clientId,clientSecret,issuer,parsedExtraScopes);
    }

    private final String generateOIDCState() {

        return generateRandomString(10);
    }

    private final String generateOIDCNonce() {

        return generateRandomString(10);
    }

    private final URI createActionUrl(AuthenticationFlowContext context) {

        String accesscode = context.generateAccessCode();
        return context.getActionUrl(accesscode);
    }


    private final void saveRealmStringData(AuthenticationFlowContext context, String key, String value) {

        context.getRealm().setAttribute(key, value);
    }

    private String generateRandomString(int length) {
        int leftlimit = 48; 
        int rightlimit = 122;
    
        return new SecureRandom().ints(leftlimit,rightlimit+1)
               .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
               .limit(length)
               .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
               .toString();
    }

    private OIDCAuthRequest createAuthnRequest(Configuration config, String state, String nonce,String redirecturi) {
        //
        OIDCAuthRequest request = new OIDCAuthRequest();
        request.setClientId(config.clientId);
        request.addScope(OPENID_SCOPE);
        request.addScope(USERNAME_SCOPE);
        request.addScope(EMAIL_SCOPE);
        for(String extrascope : config.scopes) {
            request.addScope(extrascope);
        }
        request.addResponseType(OPENID_CODE_RESPONSE);
        request.setNonce(nonce);
        request.setState(state);
        request.setRedirectUri(redirecturi);
        return request;
    }

    private OIDCTokenRequest createTokenRequest(Configuration config,String code,URI redirecturi) {

        return new OIDCTokenRequest(code,config.clientId,config.clientSecret,redirecturi);
    }

    private final Response onConfigurationError(AuthenticationFlowContext context) {

        return context.form().createForm(JANS_AUTH_ERROR_FTL);
    }

    private final Response onMissingAuthenticationCode(AuthenticationFlowContext context) {

        return context.form().createForm(JANS_AUTH_ERROR_FTL);
    }

    private final Response onTokenRetrievalError(AuthenticationFlowContext context) {

        return context.form().createForm(JANS_AUTH_ERROR_FTL);
    }

    private final Response onUserInfoRetrievalError (AuthenticationFlowContext context) {

        return context.form().createForm(JANS_AUTH_ERROR_FTL);
    }

    private final String getOpenIdCode(AuthenticationFlowContext context) {

        return context.getRealm().getAttribute(SessionAttributes.JANS_OIDC_CODE);
    }


    public static class ValidationResult {

        private List<String> errors;

        public void addError(String error) {

            if(errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.add(error);
        }

        public boolean hasErrors() {

            return this.errors != null;
        }

        public List<String> getErrors() {

            return this.errors;
        }
    }

    private class Configuration  {

        private String serverUrl;
        private String clientId;
        private String clientSecret;
        private String issuerUrl;
        private List<String> scopes;

        public Configuration(String serverUrl,String clientId, String clientSecret, String issuerUrl, List<String> scopes) {

            this.serverUrl = serverUrl;
            this.clientId  = clientId;
            this.clientSecret = clientSecret;
            this.issuerUrl = issuerUrl;
            this.scopes = scopes;
        }

        
        public ValidationResult validate() {

            ValidationResult result = new ValidationResult();

            if(serverUrl == null || serverUrl.isEmpty()) {
                result.addError("Missing or empty Server Url");
            }

            if(clientId == null || clientId.isEmpty()) {
                result.addError("Missing or empty Client ID");
            }

            if(clientSecret == null || clientSecret.isEmpty()) {
                result.addError("Missing or empty client secret");
            }
            return result;
        }

        public String normalizedIssuerUrl() {

            String effectiveUrl = issuerUrl;
            if(effectiveUrl == null) {
                effectiveUrl = serverUrl;
            }
            if(effectiveUrl == null) {
                return null;
            }
            
            if(effectiveUrl.charAt(effectiveUrl.length() -1) == '/') {
                return effectiveUrl.substring(0, effectiveUrl.length() -1);
            }
            return effectiveUrl;
        } 
        
    }
}