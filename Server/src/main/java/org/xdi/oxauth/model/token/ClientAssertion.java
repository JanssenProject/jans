/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.token;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jws.JwsValidator;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.ClientService;
import org.xdi.util.security.StringEncrypter;

import java.util.Date;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version 0.9 May 18, 2015
 */
public class ClientAssertion {

    private Jwt jwt;
    private String clientSecret;

    public ClientAssertion(String clientId, ClientAssertionType clientAssertionType, String encodedAssertion)
            throws InvalidJwtException {
        try {
            if (!load(clientId, clientAssertionType, encodedAssertion)) {
                throw new InvalidJwtException("Cannot load the JWT");
            }
        } catch (StringEncrypter.EncryptionException e) {
            throw new InvalidJwtException(e.getMessage(), e);
        }
    }

    public String getSubjectIdentifier() {
        return jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
    }

    public String getClientSecret() {
        return clientSecret;
    }

    private boolean load(String clientId, ClientAssertionType clientAssertionType, String encodedAssertion)
            throws InvalidJwtException, StringEncrypter.EncryptionException {
        boolean result = false;

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
                if ((clientId == null && StringUtils.isNotBlank(issuer) && StringUtils.isNotBlank(subject)
                        && issuer.equals(subject)) || (StringUtils.isNotBlank(clientId)
                        && StringUtils.isNotBlank(issuer)
                        && StringUtils.isNotBlank(subject)
                        && clientId.equals(issuer) && issuer.equals(subject))) {

                    // Validate audience
                    String tokenUrl = ConfigurationFactory.instance().getConfiguration().getTokenEndpoint();
                    if (audience != null && audience.contains(tokenUrl)) {

                        // Validate expiration
                        if (expirationTime.after(new Date())) {
                            ClientService clientService = (ClientService) Component.getInstance(ClientService.class);
                            Client client = clientService.getClient(subject);

                            // Validate client
                            if (client != null) {
                                JwtType jwtType = JwtType.fromString(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
                                AuthenticationMethod authenticationMethod = client.getAuthenticationMethod();
                                SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getAlgorithm();

                                if (jwtType == null && signatureAlgorithm != null) {
                                    jwtType = signatureAlgorithm.getJwtType();
                                }

                                if (jwtType == JwtType.JWS && signatureAlgorithm != null && signatureAlgorithm.getFamily() != null &&
                                        ((authenticationMethod == AuthenticationMethod.CLIENT_SECRET_JWT && signatureAlgorithm.getFamily().equals("HMAC"))
                                                || (authenticationMethod == AuthenticationMethod.PRIVATE_KEY_JWT && (signatureAlgorithm.getFamily().equals("RSA") || signatureAlgorithm.getFamily().equals("EC"))))) {
                                    clientSecret = client.getClientSecret();

                                    // Validate the crypto segment
                                    JwsValidator jwtValidator = new JwsValidator(jwt, clientSecret, client.getJwksUri(), client.getJwks());
                                    if (jwtValidator.validateSignature()) {
                                        result = true;
                                    } else {
                                        throw new InvalidJwtException("Invalid cryptographic segment");
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