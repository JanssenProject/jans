package org.gluu.oxauth.model.token;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.IAuthorizationGrant;
import org.gluu.oxauth.model.common.SubjectType;
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
import org.gluu.oxauth.model.jwt.JwtType;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.JwtUtil;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.service.PairwiseIdentifierService;
import org.gluu.oxauth.service.ServerCryptoProvider;
import org.gluu.util.StringHelper;
import org.json.JSONObject;
import org.oxauth.persistence.model.PairwiseIdentifier;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.UUID;

import static org.gluu.oxauth.model.jwt.JwtHeaderName.ALGORITHM;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class JwrService {

    @Inject
    private Logger log;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ClientService clientService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private PairwiseIdentifierService pairwiseIdentifierService;

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

    public JsonWebResponse createJwr(Client client) {
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

    public void setSubjectIdentifier(JsonWebResponse jwr, IAuthorizationGrant authorizationGrant) throws Exception {
        final Client client = authorizationGrant.getClient();
        if (client.getSubjectType() != null &&
                SubjectType.fromString(client.getSubjectType()).equals(SubjectType.PAIRWISE) &&
                (StringUtils.isNotBlank(client.getSectorIdentifierUri()) || client.getRedirectUris() != null)) {
            final String sectorIdentifierUri;
            if (StringUtils.isNotBlank(client.getSectorIdentifierUri())) {
                sectorIdentifierUri = client.getSectorIdentifierUri();
            } else {
                sectorIdentifierUri = client.getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            String clientId = authorizationGrant.getClientId();
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifierUri, clientId);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifierUri, clientId);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jwr.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            if (client.getSubjectType() != null && SubjectType.fromString(client.getSubjectType()).equals(SubjectType.PAIRWISE)) {
                log.warn("Unable to calculate the pairwise subject identifier because the client hasn't a redirect uri. A public subject identifier will be used instead.");
            }
            String openidSubAttribute = appConfiguration.getOpenidSubAttribute();
            String subValue = authorizationGrant.getUser().getAttribute(openidSubAttribute);
            if (StringHelper.equalsIgnoreCase(openidSubAttribute, "uid")) {
                subValue = authorizationGrant.getUser().getUserId();
            }
            jwr.getClaims().setSubjectIdentifier(subValue);
        }
    }

}
