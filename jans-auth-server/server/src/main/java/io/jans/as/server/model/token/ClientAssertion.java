/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.ClientAssertionType;
import io.jans.as.server.service.ClientService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class ClientAssertion {

    private Jwt jwt;
    private String clientSecret;

    public ClientAssertion(AppConfiguration appConfiguration, AbstractCryptoProvider cryptoProvider, String clientId, io.jans.as.model.token.ClientAssertionType clientAssertionType, String encodedAssertion)
            throws InvalidJwtException {
        try {
            if (!load(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion)) {
                throw new InvalidJwtException("Cannot load the JWT");
            }
        } catch (StringEncrypter.EncryptionException e) {
            throw new InvalidJwtException(e.getMessage(), e);
        } catch (Exception e) {
            throw new InvalidJwtException("Cannot verify the JWT", e);
        }
    }

    public String getSubjectIdentifier() {
        return jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
    }

    public String getClientSecret() {
        return clientSecret;
    }

    private boolean load(AppConfiguration appConfiguration, AbstractCryptoProvider cryptoProvider, String clientId, io.jans.as.model.token.ClientAssertionType clientAssertionType, String encodedAssertion)
            throws Exception {
        boolean result;

        if (clientAssertionType == ClientAssertionType.JWT_BEARER) {
            if (StringUtils.isNotBlank(encodedAssertion)) {
                jwt = Jwt.parse(encodedAssertion);

                // TODO: Store jti this value to check for duplicates

                // Validate clientId
                String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
                String subject = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
                List<String> audience = jwt.getClaims().getClaimAsStringList(JwtClaimName.AUDIENCE);
                Date expirationTime = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
                //SignatureAlgorithm algorithm = SignatureAlgorithm.fromName(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
                if ((clientId == null && StringUtils.isNotBlank(issuer) && StringUtils.isNotBlank(subject) && issuer.equals(subject))
                        || (StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(issuer)
                        && StringUtils.isNotBlank(subject) && clientId.equals(issuer) && issuer.equals(subject))) {

                    // Validate audience
                    String tokenUrl = appConfiguration.getTokenEndpoint();
                    String parUrl = StringUtils.isNotBlank(appConfiguration.getParEndpoint()) ? appConfiguration.getParEndpoint() : "";
                    String cibaAuthUrl = appConfiguration.getBackchannelAuthenticationEndpoint();
                    if (audience != null && (audience.contains(appConfiguration.getIssuer()) || audience.contains(tokenUrl) || audience.contains(parUrl) || audience.contains(cibaAuthUrl))) {

                        // Validate expiration
                        if (expirationTime.after(new Date())) {
                            ClientService clientService = CdiUtil.bean(ClientService.class);
                            Client client = clientService.getClient(subject);

                            // Validate client
                            if (client != null) {
                                JwtType jwtType = JwtType.fromString(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
                                AuthenticationMethod authenticationMethod = client.getAuthenticationMethod();
                                SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();

                                if (jwtType == null && signatureAlgorithm != null) {
                                    jwtType = signatureAlgorithm.getJwtType();
                                }

                                if (jwtType != null && signatureAlgorithm != null && signatureAlgorithm.getFamily() != null &&
                                        ((authenticationMethod == AuthenticationMethod.CLIENT_SECRET_JWT && AlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily()))
                                                || (authenticationMethod == AuthenticationMethod.PRIVATE_KEY_JWT && (AlgorithmFamily.RSA.equals(signatureAlgorithm.getFamily()) || AlgorithmFamily.EC.equals(signatureAlgorithm.getFamily()))))) {
                                    if (client.getTokenEndpointAuthSigningAlg() == null || SignatureAlgorithm.fromString(client.getTokenEndpointAuthSigningAlg()).equals(signatureAlgorithm)) {
                                        clientSecret = clientService.decryptSecret(client.getClientSecret());

                                        // Validate the crypto segment
                                        String keyId = jwt.getHeader().getKeyId();
                                        JSONObject jwks = CommonUtils.getJwks(client);
                                        String sharedSecret = clientService.decryptSecret(client.getClientSecret());
                                        boolean validSignature = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(),
                                                keyId, jwks, sharedSecret, signatureAlgorithm);

                                        if (validSignature) {
                                            result = true;
                                        } else {
                                            throw new InvalidJwtException("Invalid cryptographic segment");
                                        }
                                    } else {
                                        throw new InvalidJwtException("Invalid signing algorithm");
                                    }
                                } else {
                                    throw new InvalidJwtException("Invalid authentication method");
                                }
                            } else {
                                throw new InvalidJwtException("Invalid client");
                            }
                        } else {
                            throw new InvalidJwtException("JWT has expired");
                        }
                    } else {
                        throw new InvalidJwtException("Invalid audience: " + audience);
                    }
                } else {
                    throw new InvalidJwtException("Invalid clientId");
                }
            } else {
                throw new InvalidJwtException("The Client Assertion is null or empty");
            }
        } else {
            throw new InvalidJwtException("Invalid Client Assertion Type");
        }

        return result;
    }
}