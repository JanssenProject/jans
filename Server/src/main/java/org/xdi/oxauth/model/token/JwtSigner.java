/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.token;

import org.python.jline.internal.Preconditions;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.registration.Client;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */

public class JwtSigner {

    private AbstractCryptoProvider cryptoProvider;
    private SignatureAlgorithm signatureAlgorithm;
    private String audience;
    private String hmacSharedSecret;

    private AppConfiguration appConfiguration;
    private JSONWebKeySet webKeys;

    private Jwt jwt;

    public JwtSigner(AppConfiguration appConfiguration, JSONWebKeySet webKeys, SignatureAlgorithm signatureAlgorithm, String audience) throws Exception {
        this(appConfiguration, webKeys, signatureAlgorithm, audience, null);
    }

    public JwtSigner(AppConfiguration appConfiguration, JSONWebKeySet webKeys, SignatureAlgorithm signatureAlgorithm, String audience, String hmacSharedSecret) throws Exception {
    	this.appConfiguration = appConfiguration;
    	this.webKeys = webKeys;
        this.signatureAlgorithm = signatureAlgorithm;
        this.audience = audience;
        this.hmacSharedSecret = hmacSharedSecret;

        cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration);
    }

    public static JwtSigner newJwtSigner(AppConfiguration appConfiguration, JSONWebKeySet webKeys, Client client) throws Exception {
        Preconditions.checkNotNull(client);

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm());
        if (client.getIdTokenSignedResponseAlg() != null) {
            signatureAlgorithm = SignatureAlgorithm.fromString(client.getIdTokenSignedResponseAlg());
        }
        return new JwtSigner(appConfiguration, webKeys, signatureAlgorithm, client.getClientId(), client.getClientSecret());
    }

    public Jwt newJwt() throws Exception {
        jwt = new Jwt();

        // Header
        String keyId = cryptoProvider.getKeyId(webKeys, signatureAlgorithm);
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

    public Jwt sign() throws Exception {
        // Signature
        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), hmacSharedSecret, signatureAlgorithm);
        jwt.setEncodedSignature(signature);

        return jwt;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
}
