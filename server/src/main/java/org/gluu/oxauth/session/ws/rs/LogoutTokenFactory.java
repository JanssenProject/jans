package org.gluu.oxauth.session.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.token.JsonWebResponse;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.claims.Audience;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.token.JwrService;
import org.gluu.oxauth.service.SectorIdentifierService;
import org.json.JSONObject;
import org.msgpack.core.Preconditions;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version April 10, 2020
 */
@Stateless
@Named
public class LogoutTokenFactory {

    private static final String EVENTS_KEY = "http://schemas.openid.net/event/backchannel-logout";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private JwrService jwrService;

    @Inject
    private SectorIdentifierService sectorIdentifierService;

    public JsonWebResponse createLogoutToken(Client rpClient, String outsideSid, User user) {
        try {
            Preconditions.checkNotNull(rpClient);

            JsonWebResponse jwr = jwrService.createJwr(rpClient);

            fillClaims(jwr, rpClient, outsideSid, user);

            jwrService.encode(jwr, rpClient);
            return jwr;
        } catch (Exception e) {
            log.error("Failed to create logout_token for client:" + rpClient.getClientId());
            return null;
        }
    }

    private void fillClaims(JsonWebResponse jwr, Client client, String outsideSid, User user) {
        int lifeTime = appConfiguration.getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwr.getClaims().setExpirationTime(expiration);
        jwr.getClaims().setIssuedAt(issuedAt);
        jwr.getClaims().setIssuer(appConfiguration.getIssuer());
        jwr.getClaims().setJwtId(UUID.randomUUID());
        jwr.getClaims().setClaim("events", getLogoutTokenEvents());
        Audience.setAudience(jwr.getClaims(), client);

        if (StringUtils.isNotBlank(outsideSid) && client.getAttributes().getBackchannelLogoutSessionRequired()) {
            jwr.getClaims().setClaim("sid", outsideSid);
        }

        final String sub = sectorIdentifierService.getSub(client, user, false);
        if (StringUtils.isNotBlank(sub)) {
            jwr.getClaims().setSubjectIdentifier(sub);
        }
    }

    private JSONObject getLogoutTokenEvents() {
        final JSONObject events = new JSONObject();
        events.put(EVENTS_KEY, new JSONObject());
        return events;
    }
}
