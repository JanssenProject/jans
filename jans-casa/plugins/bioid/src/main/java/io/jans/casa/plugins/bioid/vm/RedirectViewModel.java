package io.jans.casa.plugins.bioid.vm;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Base64;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.QueryParam;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Sessions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.StandardCharset;
import com.nimbusds.oauth2.sdk.GeneralException;

import io.jans.casa.conf.OIDCClientSettings;
import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import io.jans.casa.plugins.bioid.BioIdService;
import io.jans.casa.service.IPersistenceService;
import io.jans.casa.service.ISessionContext;
import io.jans.inbound.oauth2.CodeGrantUtil;
import io.jans.inbound.oauth2.OAuthParams;
import io.jans.util.Pair;

public class RedirectViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ISessionContext sessionContext;
    private String text;
    private String title;
    private String serverUrl;
    private BioIdService bis;
    private User user;
    private ObjectMapper mapper;

    public RedirectViewModel() {
        bis = BioIdService.getInstance();
        serverUrl = Utils.managedBean(IPersistenceService.class).getIssuerUrl();
        sessionContext = Utils.managedBean(ISessionContext.class);
        user = sessionContext.getLoggedUser();
        mapper = new ObjectMapper();
    }

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }

    @Init
    public void init(@QueryParam("start") String start) {
        try {
            String bioIdCode = storeBioIdCode();
            String currentUrl = WebUtils.getServletRequest().getRequestURL().toString();
            title = Labels.getLabel("general.error.general");
            CodeGrantUtil cgu = new CodeGrantUtil(
                    makeOAuthParams(bis.getCasaClient(), bioIdCode, currentUrl));
            if (Utils.isNotEmpty(start)) {
                String agamaUrl = getAuthzRequestRedirectUrl(cgu);
                WebUtils.execRedirect(agamaUrl, false);
            } else {
                String state = Optional.ofNullable(Sessions.getCurrent().getAttribute("st"))
                        .map(Object::toString).orElse(null);
                if (state == null) {
                    return;
                }
                Map<String, String[]> params = WebUtils.getServletRequest().getParameterMap();
                for (Map.Entry<String, String[]> entry : params.entrySet()) {
                    String key = entry.getKey();
                    String value = Arrays.toString(entry.getValue());
                    logger.info("Key: " + key + ", value: " + value);
                }
                String incomingState = params.get("state")[0];
                if (!incomingState.equals(state)) {
                    throw new GeneralException("State mismatch");
                }
                title = Labels.getLabel("bioid_success");
                text = Labels.getLabel("bioid_close");
            }

        } catch (Exception e) {
            text = e.getMessage();
            logger.error(text, e);
        }
    }

    private String storeBioIdCode() {
        byte size = 25;
        String code = bis.generateBioIdCode(size);
        Map<String, Object> bioIdDict = new HashMap<>();
        bioIdDict.put("code", code);
        bioIdDict.put("expiration", new Date().getTime() + 60000);
        bis.setBioIdCode(user.getId(), bioIdDict);
        logger.debug("BioID code stored successfully");
        return code;
    }

    private OAuthParams makeOAuthParams(OIDCClientSettings cl, String bioIdCode, String redirectUri) {

        OAuthParams p = new OAuthParams();
        p.setAuthzEndpoint(serverUrl + "/jans-auth/restv1/authorize");
        p.setTokenEndpoint(serverUrl + "/jans-auth/restv1/token");
        p.setClientId(cl.getClientId());
        p.setClientSecret(cl.getClientSecret());
        p.setScopes(Collections.singletonList("openid"));
        p.setRedirectUri(redirectUri);

        Map<String, String> custMap = new HashMap<>();

        custMap.put("acr_values", makeAgamaFlowParam(bioIdCode));

        // prompt is needed because the user could have previously linked an account and
        // in a new attempt to link at a different provider, launching an authn request 
        // will not trigger the agama flow because there is "existing" session in the AS
        custMap.put("prompt", "login");
        p.setCustParamsAuthReq(custMap);
        return p;

    }

    private String getAuthzRequestRedirectUrl(CodeGrantUtil cgu) throws URISyntaxException {

        logger.info("Building an agama authentication request");
        Pair<String, String> pair = cgu.makeAuthzRequest();

        Sessions.getCurrent().setAttribute("st", pair.getSecond());
        return pair.getFirst();

    }

    private String makeAgamaFlowParam(String bioIdCode) {
        String temp = null;
        try {
            temp = mapper.writeValueAsString(Map.of(
                    "bioid_enrollment_code", bioIdCode,
                    "login_hint", user.getUserName()));
            temp = new String(Base64.getUrlEncoder().encode(temp.getBytes(UTF_8)), UTF_8);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return "agama_io.jans.agama.bioid.enroll-" + temp;
    }

}
