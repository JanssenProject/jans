package io.jans.as.server.userinfo.ws.rs;

import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.util.IdUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class UserInfoService {

    @Inject
    private Logger log;

    public void fillJwr(JsonWebResponse jwr, AuthorizationGrant authorizationGrant) {
        final String clientId = authorizationGrant.getClientId();

        jwr.getClaims().setClaim("jti", IdUtil.randomShortUUID());

        if (StringUtils.isNotBlank(clientId)) {
            jwr.getClaims().setClaim("client_id", clientId);
        }
    }
}
