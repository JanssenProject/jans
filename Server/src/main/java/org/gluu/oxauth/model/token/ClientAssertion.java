/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.token;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.CryptoProviderFactory;
import org.gluu.oxauth.model.crypto.signature.AlgorithmFamily;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.jwt.JwtType;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.token.ClientAssertionType;
import org.gluu.oxauth.model.util.JwtUtil;
import org.gluu.oxauth.service.ClientService;
import org.gluu.service.cdi.util.CdiUtil;
import org.gluu.util.security.StringEncrypter;

import java.util.Date;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class ClientAssertion {

    private Jwt jwt;
    private String clientSecret;

    public ClientAssertion(AppConfiguration appConfiguration, String clientId, ClientAssertionType clientAssertionType, String encodedAssertion)
            throws InvalidJwtException {
        try {
            if (!load(appConfiguration, clientId, clientAssertionType, encodedAssertion)) {
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

    private boolean load(AppConfiguration appConfiguration, String clientId, ClientAssertionType clientAssertionType, String encodedAssertion)
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
                    if (audience != null && audience.contains(tokenUrl)) {

                        // Validate expiration
                        if (expirationTime.after(new Date())) {
                            ClientService clientService = CdiUtil.bean(ClientService.class);
                            Client client = clientService.getClient(subject);

                            // Validate client
                            if (client != null) {
                                JwtType jwtType = JwtType.fromString(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
                                AuthenticationMethod authenticationMethod = client.getAuthenticationMethod();
                                SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getAlgorithm();

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
                                        JSONObject jwks = Strings.isNullOrEmpty(client.getJwks()) ?
                                                JwtUtil.getJSONWebKeys(client.getJwksUri()) :
                                                new JSONObject(client.getJwks());
                                        String sharedSecret = clientService.decryptSecret(client.getClientSecret());
                                        AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(
                                                appConfiguration);
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
                        throw new InvalidJwtException("Invalid audience: " + audience + ", tokenUrl: " + tokenUrl);
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