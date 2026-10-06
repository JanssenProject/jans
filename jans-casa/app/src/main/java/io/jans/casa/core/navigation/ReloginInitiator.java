package io.jans.casa.core.navigation;

import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.jans.casa.core.AuthFlowContext;
import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.core.OIDCFlowService;
import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.Pair;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.Initiator;

public class ReloginInitiator implements Initiator {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doInit(Page page, Map<String, Object> map) throws Exception {

        try {
            logger.info("Forcing re-login");
            List<String> acrs = Collections.singletonList(ConfigurationHandler.AGAMA_FLOW_ACR);
            CodeVerifier codeVerifier = new CodeVerifier();
            Pair<String, String> pair = Utils.managedBean(OIDCFlowService.class).getAuthnRequestUrl(acrs, "login", codeVerifier);

            AuthFlowContext flowContext = Utils.managedBean(AuthFlowContext.class);
            flowContext.setState(pair.getY());
            flowContext.setCodeVerifier(codeVerifier.getValue());
            WebUtils.execRedirect(pair.getX());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

}
