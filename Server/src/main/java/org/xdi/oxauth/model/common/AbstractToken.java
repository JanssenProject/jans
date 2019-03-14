/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.token.HandleTokenFactory;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.JwtUtil;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>
 * Base class for the access token, refresh token and authorization code.
 * </p>
 * <p>
 * When created, a token is valid for a given lifetime, and after this period of
 * time, it will be marked as expired automatically by a background process.
 * </p>
 * <p>
 * When required, the token can be marked as revoked.
 * </p>
 *
 * @author Javier Rojas Blum
 * @version March 14, 2019
 */
public abstract class AbstractToken implements Serializable {

    @LdapAttribute(name = "oxAuthTokenCode")
    private String code;
    @LdapAttribute(name = "oxAuthCreation")
    private Date creationDate;
    @LdapAttribute(name = "oxAuthExpiration")
    private Date expirationDate;
    private boolean revoked;
    private boolean expired;

    @LdapAttribute(name = "oxAuthenticationMode")
    private String authMode;

    private String sessionDn;
    private String x5ts256;

    /**
     * Creates and initializes the values of an abstract token.
     *
     * @param lifeTime The life time of the token.
     */
    public AbstractToken(int lifeTime) {
        if (lifeTime <= 0) {
            throw new IllegalArgumentException("Lifetime of the token is less or equal to zero.");
        }
        Calendar calendar = Calendar.getInstance();
        creationDate = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        expirationDate = calendar.getTime();

        code = HandleTokenFactory.generateHandleToken();

        revoked = false;
        expired = false;
    }

    protected AbstractToken(String code, Date creationDate, Date expirationDate) {
        this.code = code;
        this.creationDate = creationDate;
        this.expirationDate = expirationDate;

        checkExpired();
    }

    /**
     * Checks whether the token has expired and if true, marks itself as expired.
     */
    public void checkExpired() {
        checkExpired(new Date());
    }

    /**
     * Checks whether the token has expired and if true, marks itself as expired.
     */
    public void checkExpired(Date now) {
        if (now.after(expirationDate)) {
            expired = true;
        }
    }

    /**
     * Checks whether a token is valid, it is valid if it is not revoked and not
     * expired.
     *
     * @return Returns <code>true</code> if the token is valid.
     */
    public boolean isValid() {
        return !revoked && !expired;
    }

    /**
     * Returns the token code.
     *
     * @return The Code of the token.
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the token code.
     *
     * @param code The code of the token.
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Returns the creation date of the token.
     *
     * @return The creation date.
     */
    public Date getCreationDate() {
        return creationDate != null ? new Date(creationDate.getTime()) : null;
    }

    /**
     * Sets the creation date of the token.
     *
     * @param creationDate The creation date.
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate != null ? new Date(creationDate.getTime()) : null;
    }

    /**
     * Returns the expiration date of the token.
     *
     * @return The expiration date.
     */
    public Date getExpirationDate() {
        return expirationDate != null ? new Date(expirationDate.getTime()) : null;
    }

    /**
     * Sets the expiration date of the token.
     *
     * @param expirationDate The expiration date.
     */
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate != null ? new Date(expirationDate.getTime()) : null;
    }

    /**
     * Returns <code>true</code> if the token has been revoked.
     *
     * @return <code>true</code> if the token has been revoked.
     */
    public boolean isRevoked() {
        return revoked;
    }

    /**
     * Sets the value of the revoked flag to indicate whether the token has been
     * revoked.
     *
     * @param revoked Revoke or not.
     */
    public synchronized void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    /**
     * Return <code>true</code> if the token has expired.
     *
     * @return <code>true</code> if the token has expired.
     */
    public boolean isExpired() {
        return expired;
    }

    /**
     * Sets the value of the expired flag to indicate whether the token has
     * expired.
     *
     * @param expired Expire or not.
     */
    public synchronized void setExpired(boolean expired) {
        this.expired = expired;
    }

    /**
     * Returns the authentication mode.
     *
     * @return The authentication mode.
     */
    public String getAuthMode() {
        return authMode;
    }

    /**
     * Sets the authentication mode.
     *
     * @param authMode The authentication mode.
     */
    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getX5ts256() {
        return x5ts256;
    }

    public void setX5ts256(String x5ts256) {
        this.x5ts256 = x5ts256;
    }

    public String getSessionDn() {
        return sessionDn;
    }

    public void setSessionDn(String sessionDn) {
        this.sessionDn = sessionDn;
    }

    /**
     * Returns the lifetime in seconds of the token.
     *
     * @return The lifetime in seconds of the token.
     */
    public int getExpiresIn() {
        int expiresIn = 0;

        checkExpired();
        if (isValid()) {
            long diff = expirationDate.getTime() - new Date().getTime();
            expiresIn = diff != 0 ? (int) (diff / 1000) : 0;
        }

        return expiresIn;
    }

    public static String getHash(String input, SignatureAlgorithm signatureAlgorithm) {
        String hash = null;

        try {
            byte[] digest;
            if (signatureAlgorithm == SignatureAlgorithm.HS256 ||
                    signatureAlgorithm == SignatureAlgorithm.RS256 ||
                    signatureAlgorithm == SignatureAlgorithm.ES256) {
                digest = JwtUtil.getMessageDigestSHA256(input);
            } else if (signatureAlgorithm == SignatureAlgorithm.HS384 ||
                    signatureAlgorithm == SignatureAlgorithm.RS384 ||
                    signatureAlgorithm == SignatureAlgorithm.ES512) {
                digest = JwtUtil.getMessageDigestSHA384(input);
            } else if (signatureAlgorithm == SignatureAlgorithm.HS512 ||
                    signatureAlgorithm == SignatureAlgorithm.RS384 ||
                    signatureAlgorithm == SignatureAlgorithm.ES512) {
                digest = JwtUtil.getMessageDigestSHA512(input);
            } else { // Default
                digest = JwtUtil.getMessageDigestSHA256(input);
            }

            if (digest != null) {
                byte[] lefMostHalf = new byte[digest.length / 2];
                System.arraycopy(digest, 0, lefMostHalf, 0, lefMostHalf.length);
                hash = Base64Util.base64urlencode(lefMostHalf);
            }
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        } catch (NoSuchProviderException e) {
        } catch (Exception e) {
        }

        return hash;
    }
}