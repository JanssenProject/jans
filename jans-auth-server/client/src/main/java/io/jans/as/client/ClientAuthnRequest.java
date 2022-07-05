/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.ClientAssertionType;
import io.jans.as.model.util.QueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 */
public abstract class ClientAuthnRequest extends BaseRequest {

    private static final Logger LOG = Logger.getLogger(ClientAuthnRequest.class);

    private SignatureAlgorithm algorithm;
    private String sharedKey;
    private String audience;
    private AbstractCryptoProvider cryptoProvider;
    private String keyId;

    public AbstractCryptoProvider getCryptoProvider() {
        return cryptoProvider;
    }

    public void setCryptoProvider(AbstractCryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public SignatureAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(SignatureAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public void appendClientAuthnToQuery(QueryBuilder builder) {
        if (getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_POST) {
            builder.append("client_id", getAuthUsername());
            builder.append("client_secret", getAuthPassword());
        } else if (getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_JWT ||
                getAuthenticationMethod() == AuthenticationMethod.PRIVATE_KEY_JWT) {
            builder.append("client_assertion_type", ClientAssertionType.JWT_BEARER.toString());
            builder.append("client_assertion", getClientAssertion());
        }
    }

    public SignatureAlgorithm getFallbackAlgorithm() {
        return StringUtils.isBlank(keyId) ? SignatureAlgorithm.HS256 : SignatureAlgorithm.RS256;
    }

    public String getClientAssertion() {
        if (cryptoProvider == null) {
            LOG.error("Crypto provider is not specified");
            return null;
        }

        if (algorithm == null) {
            algorithm = getFallbackAlgorithm();
        }

        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.MINUTE, 5);
        Date expirationTime = calendar.getTime();

        Jwt clientAssertion = new Jwt();
        // Header
        clientAssertion.getHeader().setType(JwtType.JWT);
        clientAssertion.getHeader().setAlgorithm(algorithm);
        if (StringUtils.isNotBlank(keyId)) {
            clientAssertion.getHeader().setKeyId(keyId);
        }

        // Claims
        clientAssertion.getClaims().setIssuer(getAuthUsername());
        clientAssertion.getClaims().setSubjectIdentifier(getAuthUsername());
        clientAssertion.getClaims().setAudience(audience);
        clientAssertion.getClaims().setJwtId(UUID.randomUUID());
        clientAssertion.getClaims().setExpirationTime(expirationTime);
        clientAssertion.getClaims().setIssuedAt(issuedAt);

        // Signature
        try {
            if (sharedKey == null) {
                sharedKey = getAuthPassword();
            }
            String signature = cryptoProvider.sign(clientAssertion.getSigningInput(), keyId, sharedKey, algorithm);
            clientAssertion.setEncodedSignature(signature);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return clientAssertion.toString();
    }
}
