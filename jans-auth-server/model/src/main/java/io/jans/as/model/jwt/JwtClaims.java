/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

import com.google.common.collect.Lists;
import io.jans.as.model.exception.InvalidJwtException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.jans.as.model.jwt.JwtClaimName.AUDIENCE;
import static io.jans.as.model.jwt.JwtClaimName.EXPIRATION_TIME;
import static io.jans.as.model.jwt.JwtClaimName.ISSUED_AT;
import static io.jans.as.model.jwt.JwtClaimName.ISSUER;
import static io.jans.as.model.jwt.JwtClaimName.JWT_ID;
import static io.jans.as.model.jwt.JwtClaimName.NOT_BEFORE;
import static io.jans.as.model.jwt.JwtClaimName.SUBJECT_IDENTIFIER;
import static io.jans.as.model.jwt.JwtClaimName.TYPE;

/**
 * @author Javier Rojas Blum Date: 11.09.2012
 */
public class JwtClaims extends JwtClaimSet {

    public JwtClaims() {
        super();
    }

    public JwtClaims(JSONObject jsonObject) {
        super(jsonObject);
    }

    public JwtClaims(String base64JsonObject) throws InvalidJwtException {
        super(base64JsonObject);
    }

    /**
     * Identifies the expiration time on or after which the token MUST NOT be accepted for processing.
     *
     * @param expirationTime The expiration time.
     */
    public void setExpirationTime(Date expirationTime) {
        setClaim(EXPIRATION_TIME, expirationTime);
    }

    /**
     * Identifies the time before which the token MUST NOT be accepted for processing.
     * The processing of the "nbf" claim requires that the current date/time MUST be after or equal to the not-before
     * date/time listed in the "nbf" claim.
     *
     * @param notBefore The not-before date.
     */
    public void setNotBefore(Date notBefore) {
        setClaim(NOT_BEFORE, notBefore);
    }

    public void setNbf(Date notBefore) {
        setNotBefore(notBefore);
    }

    /**
     * Identifies the time at which the JWT was issued.
     * This claim can be used to determine the age of the token.
     *
     * @param issuedAt The issue date.
     */
    public void setIssuedAt(Date issuedAt) {
        setClaim(ISSUED_AT, issuedAt);
    }

    public void setIatNow() {
        setIssuedAt(new Date());
    }

    public void setIat(Date issuedAt) {
        setIssuedAt(issuedAt);
    }

    /**
     * Identifies the principal that issued the JWT.
     *
     * @param issuer The issuer of the JWT.
     */
    public void setIssuer(String issuer) {
        setClaim(ISSUER, issuer);
    }

    /**
     * Identifies the principal that issued the JWT.
     *
     * @param issuer The issuer of the JWT.
     */
    public void setIssuer(URI issuer) {
        if (issuer == null) {
            setNullClaim(ISSUER);
        } else {
            setClaim(ISSUER, issuer.toString());
        }
    }

    public void addAudience(String audience) {
        if (StringUtils.isBlank(audience)) {
            return;
        }

        if (!hasClaim(AUDIENCE)) {
            setAudience(audience);
            return;
        }

        List<String> value = Lists.newArrayList();
        Object currentAudience = getClaim(AUDIENCE);
        if (currentAudience instanceof String) {
            value.add((String) currentAudience);
        } else if (currentAudience instanceof Collection) {
            value.addAll((Collection) currentAudience);
        }
        if (!value.contains(audience)) {
            value.add(audience);
        }

        if (value.size() == 1) {
            setAudience(value.iterator().next());
            return;
        }

        setClaim(AUDIENCE, value);
    }


    /**
     * Identifies the audience that the JWT is intended for.
     * The principal intended to process the JWT MUST be identified with the value of the audience claim.
     * If the principal processing the claim does not identify itself with the identifier in the "aud" claim
     * value then the JWT MUST be rejected.
     *
     * @param audience The audience of the JWT.
     */
    public void setAudience(String audience) {
        setClaim(AUDIENCE, audience);
    }

    /**
     * Identifies the audience that the JWT is intended for.
     * The principal intended to process the JWT MUST be identified with the value of the audience claim.
     * If the principal processing the claim does not identify itself with the identifier in the "aud" claim
     * value then the JWT MUST be rejected.
     *
     * @param audience The audience of the JWT.
     */
    public void setAudience(URI audience) {
        if (audience == null) {
            setNullClaim(AUDIENCE);
        } else {
            setClaim(AUDIENCE, audience.toString());
        }
    }

    /**
     * Identifies the subject of the JWT.
     *
     * @param subjectIdentifier The subject of the JWT.
     */
    public void setSubjectIdentifier(String subjectIdentifier) {
        setClaim(SUBJECT_IDENTIFIER, subjectIdentifier);
    }

    /**
     * Identifies the subject of the JWT.
     *
     * @param subjectIdentifier The subject of the JWT.
     */
    public void setSubjectIdentifier(URI subjectIdentifier) {
        if (subjectIdentifier == null) {
            setNullClaim(SUBJECT_IDENTIFIER);
        } else {
            setClaim(SUBJECT_IDENTIFIER, subjectIdentifier.toString());
        }
    }

    /**
     * Provides a unique identifier for the JWT.
     *
     * @param jwtId Unique identifier for the JWT.
     */
    public void setJwtId(String jwtId) {
        setClaim(JWT_ID, jwtId);
    }

    /**
     * Provides a unique identifier for the JWT.
     *
     * @param jwtId Unique identifier for the JWT.
     */
    public void setJwtId(UUID jwtId) {
        if (jwtId == null) {
            setNullClaim(JWT_ID);
        } else {
            setClaim(JWT_ID, jwtId.toString());
        }
    }

    /**
     * Declare a type for the contents of this JWT Claims Set.
     *
     * @param type The type of the JWT claims set.
     */
    public void setType(JwtType type) {
        if (type == null) {
            setNullClaim(TYPE);
        } else {
            setClaim(TYPE, type.toString());
        }
    }
}