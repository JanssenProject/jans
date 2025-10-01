package io.jans.casa.plugins.acctlinking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.ParseException;

import io.jans.casa.conf.OIDCClientSettings;
import io.jans.casa.core.model.IdentityPerson;
import io.jans.casa.misc.Utils;
import io.jans.casa.model.ApplicationConfiguration;
import io.jans.casa.plugins.acctlinking.conf.Config;
import io.jans.casa.service.IPersistenceService;
import io.jans.casa.service.settings.*;
import io.jans.inbound.Provider;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AccountsLinkingService {
    
    public static final String CASA_AGAMA_FLOW = "io.jans.casa.authn.acctlinking";

    public static final String READ_SCOPE = "https://jans.io/oauth/config/agama.readonly";
    
    private static final String AGAMA_PRJ = "casa-account-linking";
    
    private static final String CONFIGS_ENDPOINT = 
            "/jans-config-api/api/v1/agama-deployment/configs/" + AGAMA_PRJ;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static AccountsLinkingService instance;
    private IPersistenceService ips;
    
    private IPluginSettingsHandler<Config> settingsHandler;
    private ObjectMapper mapper;
    private OIDCClientSettings clSettings;
    private String issuer;
    private String basicAuthnHeader;    
    
    public static AccountsLinkingService getInstance(String pluginId) {
        if (instance == null && pluginId != null) {
            instance = new AccountsLinkingService(pluginId);
        }
        return instance;
    }
    
    public static AccountsLinkingService getInstance() {
        return instance;
    }

    public boolean usePopup() {
        return Optional.ofNullable(settingsHandler.getSettings()).map(Config::isUsePopup).orElse(true);
    }
    
    public OIDCClientSettings getCasaClient() {
        return clSettings;
    }
    
    public Map<String, Provider> getProviders(boolean enabledOnly) throws Exception {
        
        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.GET,
                new URL(issuer + CONFIGS_ENDPOINT));
        setTimeouts(request);        
        request.setAuthorization(basicAuthnHeader);
        request.setAuthorization("Bearer " + getAToken());

        HTTPResponse r = request.send();
        r.ensureStatusCode(200);
        
        Map<String, Map<String, Provider>> madam = mapper.readValue(
                r.getBody(), new TypeReference<Map<String, Map<String, Provider>>>(){});
                    
        Map<String, Provider> madman = Optional.ofNullable(madam)
                .map(m -> m.get(CASA_AGAMA_FLOW)).orElse(Collections.emptyMap());
                
        if (enabledOnly) {
            madman = madman.entrySet().stream().filter(e -> e.getValue().isEnabled())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return madman;
        
    }
    
    public Map<String, String> getAccounts(String userId, Set<String> knownProviders) {
        
        Map<String, String> accts = new HashMap<>();
        for (String extUid : getPerson(userId).getJansExtUid()) {
            //See method computeExtUid            
            int i = extUid.indexOf(":");
            
            if (i > 0 && i < extUid.length() - 1) {
                String pref = extUid.substring(0, i);
                if (knownProviders.contains(pref)) {
                    accts.put(pref, extUid.substring(i + 1));
                }
            }
        }
        return accts;

    }
    
    /*
    //linking occurs at the Agama flow
    public boolean link(String userId, String providerId, String extUid) {
        
        try {
            IdentityPerson p = getPerson(userId);
            List<String> extUids = new ArrayList<>(p.getJansExtUid());
            extUids.add(computeExtUid(providerId, extUid));
            
            p.setJansExtUid(extUids);
            ips.modify(p);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;

    }*/
    
    public boolean delink(String userId, String providerId, String extUid) {
        
        try {
            IdentityPerson p = getPerson(userId);
            List<String> extUids = new ArrayList<>(p.getJansExtUid());
            extUids.remove(computeExtUid(providerId, extUid));
            
            p.setJansExtUid(extUids);
            ips.modify(p);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;

    }

    public boolean hasPassword(String id) {
        return getPerson(id).hasPassword();
    }
    
    private IdentityPerson getPerson(String id) {
        return ips.get(IdentityPerson.class, ips.getPersonDn(id));   
    }
    
    private String computeExtUid(String providerId, String id) {
        //This method HAS to match computeExtUid in class io.jans.agama.inbound.Utils 
        return providerId + ":" + id;
    }
    
    private AccountsLinkingService(String pluginId) {
        logger.info("Initializing AccountsLinkingService");
        settingsHandler = Utils.managedBean(IPluginSettingsHandlerFactory.class)
                .getHandler(pluginId, Config.class);
        mapper = new ObjectMapper();
        
        ips = Utils.managedBean(IPersistenceService.class);        
        issuer = ips.getIssuerUrl();
        logger.debug("Issuer is {}", issuer);
        
        clSettings = ips.get(ApplicationConfiguration.class, "ou=casa,ou=configuration,o=jans")
                .getSettings().getOidcSettings().getClient();
        
        String authz = clSettings.getClientId() + ":" + clSettings.getClientSecret();
        authz = new String(Base64.getEncoder().encode(authz.getBytes(UTF_8)), UTF_8);
        basicAuthnHeader = "Basic " + authz;
        
    }

    private String getAToken() throws IOException {

        StringJoiner joiner = new StringJoiner("&");
        Map.of("grant_type", "client_credentials", "scope", URLEncoder.encode(READ_SCOPE, UTF_8))
                .forEach((k, v) -> joiner.add(k + "=" + v));

        logger.info("Calling token endpoint");

        HTTPRequest request = new HTTPRequest(
                HTTPRequest.Method.POST, new URL(issuer + "/jans-auth/restv1/token"));
        setTimeouts(request);
        request.setQuery(joiner.toString());
        request.setAuthorization(basicAuthnHeader);

        try {
            Map<String, Object> jobj = request.send().getContentAsJSONObject();
            logger.info("Successful call");
            return jobj.get("access_token").toString();
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }

    }

    private void setTimeouts(HTTPRequest request) {
        request.setConnectTimeout(3500);
        request.setReadTimeout(3500);
    }
    
}
