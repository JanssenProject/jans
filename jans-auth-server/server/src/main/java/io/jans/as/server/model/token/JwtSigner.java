/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

import com.google.common.base.Preconditions;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.KeyOpsType;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ServerCryptoProvider;
import io.jans.service.cdi.util.CdiUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */

public class JwtSigner {

    private final static Logger log = LoggerFactory.getLogger(JwtSigner.class);

    private AbstractCryptoProvider cryptoProvider;
    private final SignatureAlgorithm signatureAlgorithm;
    private final String audience;
    private final String hmacSharedSecret;

    private final AppConfiguration appConfiguration;
    private final JSONWebKeySet webKeys;

    private Jwt jwt;

    public JwtSigner(AppConfiguration appConfiguration, JSONWebKeySet webKeys, SignatureAlgorithm signatureAlgorithm, String audience) {
        this(appConfiguration, webKeys, signatureAlgorithm, audience, null);
    }

    public JwtSigner(AppConfiguration appConfiguration, JSONWebKeySet webKeys, SignatureAlgorithm signatureAlgorithm, String audience, String hmacSharedSecret) {
        this(appConfiguration, webKeys, signatureAlgorithm, audience, hmacSharedSecret, null);
    }

    public JwtSigner(AppConfiguration appConfiguration, JSONWebKeySet webKeys, SignatureAlgorithm signatureAlgorithm, String audience, String hmacSharedSecret, AbstractCryptoProvider cryptoProvider) {
        this.appConfiguration = appConfiguration;
        this.webKeys = webKeys;
        this.signatureAlgorithm = signatureAlgorithm;
        this.audience = audience;
        this.hmacSharedSecret = hmacSharedSecret;

        this.cryptoProvider = cryptoProvider != null ? cryptoProvider : new ServerCryptoProvider(CdiUtil.bean(AbstractCryptoProvider.class));
    }

    public static JwtSigner newJwtSigner(AppConfiguration appConfiguration, JSONWebKeySet webKeys, Client client) throws Exception {
        Preconditions.checkNotNull(client);

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm());
        if (client.getIdTokenSignedResponseAlg() != null) {
            signatureAlgorithm = SignatureAlgorithm.fromString(client.getIdTokenSignedResponseAlg());
        }

        ClientService clientService = CdiUtil.bean(ClientService.class);
        return new JwtSigner(appConfiguration, webKeys, signatureAlgorithm, client.getClientId(), clientService.decryptSecret(client.getClientSecret()));
    }

    public Jwt newJwt() throws Exception {
        jwt = new Jwt();

        // Header
        String keyId = getKid();
        if (keyId != null) {
            jwt.getHeader().setKeyId(keyId);
        }
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        // Claims
        jwt.getClaims().setIssuer(appConfiguration.getIssuer());
        jwt.getClaims().setAudience(audience);
        return jwt;
    }

    private String getKid() throws CryptoProviderException {
        final String staticKid = appConfiguration.getStaticKid();
        if (StringUtils.isNotBlank(staticKid)) {
            log.trace("Use staticKid: {}", staticKid);
            return staticKid;
        }

        return cryptoProvider.getKeyId(webKeys, Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE, KeyOpsType.CONNECT);
    }

    public Jwt sign() throws Exception {
        // Signature
        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), hmacSharedSecret, signatureAlgorithm);
        jwt.setEncodedSignature(signature);

        return jwt;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setCryptoProvider(AbstractCryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }
}