package io.jans.casa.plugins.acctlinking.vm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.jans.as.model.util.Base64Util;
import io.jans.casa.conf.OIDCClientSettings;
import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import io.jans.casa.service.IPersistenceService;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.plugins.acctlinking.AccountsLinkingService;
import io.jans.inbound.oauth2.OAuthParams;
import io.jans.inbound.oauth2.CodeGrantUtil;
import io.jans.service.cache.*;
import io.jans.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.net.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.QueryParam;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SiteRedirectVM {

    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final String STATE_ATTR = "st";

    @WireVariable
    private ISessionContext sessionContext;
    
    private CacheInterface cache;    
    private AccountsLinkingService als;
    private ObjectMapper mapper;

    private String serverUrl;
    private String userName; 

    private String text;
    private String title;

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }

    public SiteRedirectVM() {
        als = AccountsLinkingService.getInstance();    
        mapper = new ObjectMapper();
        serverUrl = Utils.managedBean(IPersistenceService.class).getIssuerUrl();
        cache = Utils.managedBean(CacheInterface.class);
    }
    
    @Init
    public void init(@QueryParam("provider") String provider) {

        try {
            logger.debug("Initializing ViewModel");            
            userName = sessionContext.getLoggedUser().getUserName();
        
            title = Labels.getLabel("general.error.general");
            String currentUrl = WebUtils.getServletRequest().getRequestURL().toString();

            CodeGrantUtil cgu = new CodeGrantUtil(makeOAuthParams(als.getCasaClient(), provider, currentUrl));
            
            if (Utils.isNotEmpty(provider)) {
                text = Labels.getLabel("al.link_redirect_failed", new String[]{ provider });
                String url = getAuthzRequestRedirectUrl(cgu);

                EventQueues.lookup(AccountsLinkingVM.LINK_QUEUE, EventQueues.SESSION, true)
                        .publish(new Event(AccountsLinkingVM.EVENT_NAME, null, provider));
                WebUtils.execRedirect(url, false);
                
            } else {

                //Agama authn flow finished
                String state = Optional.ofNullable(Sessions.getCurrent().getAttribute(STATE_ATTR))
                        .map(Object::toString).orElse(null);

                if (state == null) return;

                Map mama = WebUtils.getServletRequest().getParameterMap().entrySet().stream()
                        .map(entry -> {
                                String[] val = entry.getValue();
                                List<String> values = (val == null || val.length == 0) ?
                                        Collections.emptyList() : Arrays.asList(val);
                                return new AbstractMap.SimpleEntry<>(entry.getKey(), values);
                        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        
                String code = cgu.parseCode(mama, state);
                
                logger.info("Got an authz code at callback URL");
                //If the token request succeeds, ie. does not throw, it means the code is not fake
                cgu.getTokenResponse(code);
                
                logger.info("Notifying linking page...");
                EventQueues.lookup(AccountsLinkingVM.LINK_QUEUE, EventQueues.SESSION, true)
                        .publish(new Event(AccountsLinkingVM.EVENT_NAME, null, null));
                        
                title = Labels.getLabel("al.linking_result.success");
                text = Labels.getLabel("al.linking_result.success_close");
            }
    
        } catch (Exception e) {
            text = e.getMessage();
            logger.error(text, e);
        }

    }

    private String getAuthzRequestRedirectUrl(CodeGrantUtil cgu) throws URISyntaxException {
        
        logger.info("Building an agama authentication request");        
        Pair<String, String> pair = cgu.makeAuthzRequest();
        
        Sessions.getCurrent().setAttribute(STATE_ATTR, pair.getSecond());
        return pair.getFirst();
        
    }
    
    private OAuthParams makeOAuthParams(OIDCClientSettings cl, String provider, String redirectUri) {
        
        OAuthParams p = new OAuthParams();
        p.setAuthzEndpoint(serverUrl + "/jans-auth/restv1/authorize");
        p.setTokenEndpoint(serverUrl + "/jans-auth/restv1/token");
        p.setClientId(cl.getClientId());
        p.setClientSecret(cl.getClientSecret());
        p.setScopes(Collections.singletonList("openid"));
        p.setRedirectUri(redirectUri);
        
        Map<String, String> custMap = new HashMap<>();
        
        if (provider != null) {
            custMap.put("acr_values", "agama_" + als.CASA_AGAMA_FLOW + "-" + buildFlowParams(provider));
        }

        //prompt is needed because the user could have previously linked an account and in a new
        //attempt to link at a different provider, launching an authn request will not trigger the
        //agama flow because there is "existing" session in the AS
        custMap.put("prompt", "login");
        p.setCustParamsAuthReq(custMap);
        return p;

    }
    
    private String buildFlowParams(String provider) {
        
        String key = "" + Math.random();
        int sec = Long.valueOf(AccountsLinkingVM.ENROLL_TIME_MS).intValue() / 1000;
        
        logger.debug("Writing uid ref to cache");
        //What is stored here will be later retrieved by the agama flow. Twice the casa enrollment time 
        //given. This is to avoid the not very user-friendly error "uid reference passed not found in Cache!"
        cache.put(2 * sec, key, userName);
        
        String s = null;
        try {
            s = mapper.writeValueAsString(Map.of("providerId", provider, "uidRef", key));
            s = Base64Util.base64urlencode(s.getBytes(UTF_8));
        } catch (JsonProcessingException e) {
            //this will never happen
            logger.error(e.getMessage());            
        }
        return s;

    }

}
