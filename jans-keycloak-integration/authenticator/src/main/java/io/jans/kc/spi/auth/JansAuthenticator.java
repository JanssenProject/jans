package io.jans.kc.spi.auth;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import java.text.MessageFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

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

import io.jans.kc.spi.auth.oidc.OIDCAuthRequest;
import io.jans.kc.spi.auth.oidc.OIDCMetaError;
import io.jans.kc.spi.auth.oidc.OIDCService;
import io.jans.kc.spi.auth.oidc.OIDCTokenError;
import io.jans.kc.spi.auth.oidc.OIDCTokenRequest;
import io.jans.kc.spi.auth.oidc.OIDCTokenRequestError;
import io.jans.kc.spi.auth.oidc.OIDCTokenResponse;
import io.jans.kc.spi.auth.oidc.OIDCUserInfoError;
import io.jans.kc.spi.auth.oidc.OIDCUserInfoRequestError;
import io.jans.kc.spi.auth.oidc.OIDCUserInfoResponse;

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

    private static final String URI_PATH_TO_REST_SERVICE = "/realms/{0}/jans-auth-bridge/auth-complete";
    
    
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

        try {
            URI redirecturi = createRedirectUri(context);
            URI actionuri = createActionUrl(context);

            String state = generateOIDCState();
            String nonce = generateOIDCNonce();

            OIDCAuthRequest oidcauthrequest = createAuthnRequest(config, state, nonce,redirecturi.toString());
        
            URI loginurl = oidcService.createAuthorizationUrl(config.normalizedIssuerUrl(), oidcauthrequest);
            URI loginurlnoparams = UriBuilder.fromUri(loginurl.toString()).replaceQuery(null).build();
            
            Response response = context
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
            Response response = context.form().createForm(JANS_AUTH_ERROR_FTL);
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

        String openid_code = getOpenIdCode(context);
        if(openid_code == null) {
            log.errorv("Missing authentication code during response processing");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR,onMissingAuthenticationCode(context));
            return;
        }

        OIDCTokenRequest tokenrequest = createTokenRequest(config, openid_code, createRedirectUri(context));
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

        return;
    }

    @Override
    public List<RequiredActionFactory> getRequiredActions(KeycloakSession session) {

        return null;
    }

    @Override
    public void close() {

        return;
    }

    private Configuration extractAndValidateConfiguration(AuthenticationFlowContext  context) {
        
        Configuration config = pluginConfigurationFromContext(context);
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

        URI serverUri = context.getSession().getContext().getUri().getBaseUri();
        String realmname = context.getRealm().getName();
        String rest_svc_uri = MessageFormat.format(URI_PATH_TO_REST_SERVICE,realmname);
        return serverUri.resolve(rest_svc_uri);
    }

    private UserModel findUserByNameOrEmail(AuthenticationFlowContext context, String username,String email) {

        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(),context.getRealm(),username);
        if(user == null) {
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(),context.getRealm(),email);
        }
        return user;
    }


    private Map<String,String> parseQueryParameters(String params) {

        Map<String,String> ret = new HashMap<String,String>();
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

        String server_url = config.getConfig().get(JansAuthenticatorConfigProp.SERVER_URL.getName());
        String client_id  = config.getConfig().get(JansAuthenticatorConfigProp.CLIENT_ID.getName());
        String client_secret = config.getConfig().get(JansAuthenticatorConfigProp.CLIENT_SECRET.getName());
        String issuer = config.getConfig().get(JansAuthenticatorConfigProp.ISSUER.getName());
        String extra_scopes = config.getConfig().get(JansAuthenticatorConfigProp.EXTRA_SCOPES.getName());
        List<String> parsed_extra_scopes = new ArrayList<>();
        if(extra_scopes != null) {
            String [] tokens = extra_scopes.split("\\s*,\\s*");
            for(String token : tokens) {
                parsed_extra_scopes.add(token);
            }
        }

        return new Configuration(server_url,client_id,client_secret,issuer,parsed_extra_scopes);
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
    
        return new Random().ints(leftlimit,rightlimit+1)
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
                this.errors = new ArrayList<String>();
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

            String effective_url = issuerUrl;
            if(effective_url == null) {
                effective_url = serverUrl;
            }
            if(effective_url == null) {
                return null;
            }
            
            if(effective_url.charAt(effective_url.length() -1) == '/') {
                return effective_url.substring(0, effective_url.length() -1);
            }
            return effective_url;
        } 
        
    }
}