package org.gluu.oxauth.model.token;

import org.gluu.oxauth.model.config.WebKeysConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJweException;
import org.gluu.oxauth.model.jwe.Jwe;
import org.gluu.oxauth.model.jwe.JweEncrypter;
import org.gluu.oxauth.model.jwe.JweEncrypterImpl;
import org.gluu.oxauth.model.jwk.Algorithm;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;
import org.gluu.oxauth.model.jwk.Use;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.JwtUtil;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.service.ServerCryptoProvider;
import org.json.JSONObject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

import static org.gluu.oxauth.model.jwt.JwtHeaderName.ALGORITHM;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class JwrEncoder {

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ClientService clientService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;


    /**
     * Encode means encrypt for Jwe and sign for Jwt, means it's implementaiton specific but we want to abstract it.
     *
     * @return encoded Jwr
     */
    public JsonWebResponse encode(JsonWebResponse jwr, Client client) throws Exception {
        if (jwr instanceof Jwe) {
            return encryptJwe((Jwe) jwr, client);
        }
        if (jwr instanceof Jwt) {
            return signJwt((Jwt) jwr, client);
        }

        throw new IllegalArgumentException("Unknown Jwr instance.");
    }

    private Jwt signJwt(Jwt jwt, Client client) throws Exception {
        JwtSigner jwtSigner = JwtSigner.newJwtSigner(appConfiguration, webKeysConfiguration, client);
        jwtSigner.setJwt(jwt);
        jwtSigner.sign();
        return jwt;
    }

    private Jwe encryptJwe(Jwe jwe, Client client) throws Exception {
        KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(jwe.getHeader().getClaimAsString(ALGORITHM));
        final BlockEncryptionAlgorithm encryptionMethod = jwe.getHeader().getEncryptionMethod();

        if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA1_5) {
            JSONObject jsonWebKeys = JwtUtil.getJSONWebKeys(client.getJwksUri());
            String keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(JSONWebKeySet.fromJSONObject(jsonWebKeys),
                    Algorithm.fromString(keyEncryptionAlgorithm.getName()),
                    Use.ENCRYPTION);
            PublicKey publicKey = cryptoProvider.getPublicKey(keyId, jsonWebKeys, null);
            jwe.getHeader().setKeyId(keyId);

            if (publicKey == null) {
                throw new InvalidJweException("The public key is not valid");
            }

            JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, encryptionMethod, publicKey);
            return jweEncrypter.encrypt(jwe);
        }
        if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128KW || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW) {
            byte[] sharedSymmetricKey = clientService.decryptSecret(client.getClientSecret()).getBytes(StandardCharsets.UTF_8);
            JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, encryptionMethod, sharedSymmetricKey);
            return jweEncrypter.encrypt(jwe);
        }

        throw new IllegalArgumentException("Unsupported encryption algorithm: " + keyEncryptionAlgorithm);
    }
}
