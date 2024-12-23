package io.jans.as.server.userinfo.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.util.IdUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class UserInfoService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    public void fillJwr(JsonWebResponse jwr, AuthorizationGrant authorizationGrant) {
        final String clientId = authorizationGrant.getClientId();

        final int lifetime = appConfiguration.getUserInfoLifetime();

        final Calendar calendar = Calendar.getInstance();
        final Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifetime);
        final Date expiration = calendar.getTime();

        jwr.getClaims().setClaim("jti", IdUtil.randomShortUUID());
        jwr.getClaims().setExpirationTime(expiration);
        jwr.getClaims().setIat(issuedAt);
        jwr.getClaims().setNbf(issuedAt);

        if (StringUtils.isNotBlank(clientId)) {
            jwr.getClaims().setClaim("client_id", clientId);
        }
    }
}
