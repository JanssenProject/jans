package org.gluu.oxauth.session.ws.rs;

import org.gluu.oxauth.model.config.WebKeysConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.jwe.Jwe;
import org.gluu.oxauth.model.jwt.JwtType;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.token.JsonWebResponse;
import org.gluu.oxauth.model.token.JwrEncoder;
import org.gluu.oxauth.model.token.JwtSigner;
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
    private JwrEncoder jwrEncoder;

    public JsonWebResponse createLogoutToken(Client client) {
        try {
            Preconditions.checkNotNull(client);

            JsonWebResponse jwr = createJwr(client);

            fillClaims(jwr);

            jwrEncoder.encode(jwr, client);
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

        //jwr.getClaims().setSubjectIdentifier();
    }

    private JsonWebResponse createJwr(Client client) {
        try {
            if (client.getIdTokenEncryptedResponseAlg() != null
                    && client.getIdTokenEncryptedResponseEnc() != null) {
                Jwe jwe = new Jwe();

                // Header
                KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(client.getIdTokenEncryptedResponseAlg());
                BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(client.getIdTokenEncryptedResponseEnc());
                jwe.getHeader().setType(JwtType.JWT);
                jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
                jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);
                return jwe;
            } else {
                JwtSigner jwtSigner = JwtSigner.newJwtSigner(appConfiguration, webKeysConfiguration, client);
                return jwtSigner.newJwt();
            }
        } catch (Exception e) {
            log.error("Failed to create logout_token.", e);
            return null;
        }
    }
}
