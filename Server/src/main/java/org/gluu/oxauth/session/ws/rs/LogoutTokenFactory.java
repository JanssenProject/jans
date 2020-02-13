package org.gluu.oxauth.session.ws.rs;

import org.gluu.oxauth.model.config.WebKeysConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.token.JsonWebResponse;
import org.gluu.oxauth.model.token.JwrService;
import org.msgpack.core.Preconditions;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class LogoutTokenFactory {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private JwrService jwrService;

    public JsonWebResponse createLogoutToken(Client client) {
        try {
            Preconditions.checkNotNull(client);

            JsonWebResponse jwr = jwrService.createJwr(client);

            fillClaims(jwr);

            jwrService.encode(jwr, client);
            return jwr;
        } catch (Exception e) {
            log.error("Failed to create logout_token for client:" + client.getClientId());
            return null;
        }
    }

    private void fillClaims(JsonWebResponse jwr) {
        int lifeTime = appConfiguration.getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwr.getClaims().setExpirationTime(expiration);
        jwr.getClaims().setIssuedAt(issuedAt);
        jwr.getClaims().setIssuer(appConfiguration.getIssuer());

        // todo
        //jwr.getClaims().setSubjectIdentifier();
    }
}
