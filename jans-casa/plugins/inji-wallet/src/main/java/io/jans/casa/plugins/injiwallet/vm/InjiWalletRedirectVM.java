package io.jans.casa.plugins.injiwallet.vm;

import io.jans.as.model.util.Base64Util;
import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import io.jans.casa.model.ApplicationConfiguration;
import io.jans.casa.service.IPersistenceService;
import io.jans.casa.service.ISessionContext;
import io.jans.inbound.oauth2.OAuthParams;
import io.jans.inbound.oauth2.CodeGrantUtil;
import io.jans.util.Pair;

import java.util.*;
import java.net.*;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.QueryParam;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * ViewModel for Inji Wallet credential linking
 * Uses OAuth authentication with Agama flow
 */
public class InjiWalletRedirectVM {

    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final String STATE_ATTR = "inji_st";
    private static final String INJI_LINK_FLOW = "io.jans.casa.inji.link";

    @WireVariable
    private ISessionContext sessionContext;
    
    private IPersistenceService ips;
    private String serverUrl;
    private String userId; 

    private String text;
    private String title;
    private boolean success = false;

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }
    
    public boolean isSuccess() {
        return success;
    }

    public InjiWalletRedirectVM() {
        ips = Utils.managedBean(IPersistenceService.class);
        serverUrl = ips.getIssuerUrl();
    }
    
    @Init
    public void init(@QueryParam("credentialType") String credentialType) {

        try {
            userId = sessionContext.getLoggedUser().getId();
            logger.info("Inji Wallet linking for userId: {}", userId);
        
            String currentUrl = WebUtils.getServletRequest().getRequestURL().toString();
            
            if (Utils.isNotEmpty(credentialType)) {
                // Starting the linking process
                title = "Redirecting to Inji Wallet...";
                text = "Please wait while we redirect you to verify your credential.";
                
                // Notify main page that linking has started
                EventQueues.lookup(InjiWalletUserVM.LINK_QUEUE, EventQueues.SESSION, true)
                        .publish(new Event(InjiWalletUserVM.EVENT_NAME, null, credentialType));
                
                // Build and execute OAuth request
                redirect(currentUrl);
                
            } else {
                // Callback from Agama flow
                handleCallback();
            }
    
        } catch (Exception e) {
            text = "Error during credential linking: " + e.getMessage();
            logger.error(text, e);
            title = "Linking Failed";
        }

    }

    private void redirect(String redirectUri) throws URISyntaxException {
        
        // Get Casa client ID
        String casaClientId = ips.get(ApplicationConfiguration.class, "ou=casa,ou=configuration,o=jans")
                .getSettings().getOidcSettings().getClient().getClientId();
        
        // Build flow inputs: uidRef = userId
        String inputs = new JSONObject(Map.of("uidRef", userId)).toString();
        logger.debug("Flow inputs: {}", inputs);
        
        inputs = Base64Util.base64urlencode(inputs.getBytes(UTF_8));
        
        // Build OAuth parameters
        OAuthParams params = new OAuthParams();
        params.setAuthzEndpoint(serverUrl + "/jans-auth/restv1/authorize");
        params.setTokenEndpoint(serverUrl + "/jans-auth/restv1/token");
        params.setClientId(casaClientId);
        params.setScopes(Collections.singletonList("openid"));
        params.setRedirectUri(redirectUri);
        params.setCustParamsAuthReq(
            Map.of(
                "prompt", "login",
                "acr_values", "agama_" + INJI_LINK_FLOW + "-" + inputs
            )
        );
        
        CodeGrantUtil cgu = new CodeGrantUtil(params);
        Pair<String, String> pair = cgu.makeAuthzRequest();
        
        // Store state in session
        Sessions.getCurrent().setAttribute(STATE_ATTR, pair.getSecond());
        
        logger.info("Redirecting to Agama flow: {}", INJI_LINK_FLOW);
        logger.debug("Full URL: {}", pair.getFirst());
        WebUtils.execRedirect(pair.getFirst(), false);
    }
    
    private void handleCallback() {
        
        logger.info("Processing callback from Agama flow");
        
        // Check if we have an authorization code
        String code = WebUtils.getQueryParam("code");
        
        if (code != null) {
            logger.info("Authorization code received - linking successful");
            
            // Notify main page
            EventQueues.lookup(InjiWalletUserVM.LINK_QUEUE, EventQueues.SESSION, true)
                    .publish(new Event(InjiWalletUserVM.EVENT_NAME, null, null));
                    
            title = "Credential Linked Successfully";
            text = "Your credential has been linked successfully.";
            success = true;
        } else {
            // Check for error
            String error = WebUtils.getQueryParam("error");
            String errorDesc = WebUtils.getQueryParam("error_description");
            
            logger.warn("Linking failed. Error: {}, Description: {}", error, errorDesc);
            title = "Linking Failed";
            text = errorDesc != null ? errorDesc : "Failed to link credential";
        }
    }

}
