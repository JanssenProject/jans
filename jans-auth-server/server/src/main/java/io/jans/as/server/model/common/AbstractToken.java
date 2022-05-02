/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.util.HashUtil;
import io.jans.as.model.util.Util;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.model.base.Deletable;

import java.io.Serializable;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
 * @version September 30, 2021
 */
public abstract class AbstractToken implements Serializable, Deletable {

    @AttributeName(name = "tknCde", consistency = true)
    private String code;
    @AttributeName(name = "iat")
    private Date creationDate;
    @AttributeName(name = "exp")
    private Date expirationDate;
    @AttributeName(name = "del")
    private boolean deletable = true;
    private boolean revoked;
    private boolean expired;

    @AttributeName(name = "ssnId")
    private String sessionDn;
    private String x5ts256;

    @AttributeName(name = "dpop")
    private String dpop;

    @Expiration
    private int ttl;

    /**
     * Creates and initializes the values of an abstract token.
     *
     * @param lifeTime The life time of the token.
     */
    protected AbstractToken(int lifeTime) {
        if (lifeTime <= 0) {
            throw new IllegalArgumentException("Lifetime of the token is less or equal to zero.");
        }
        ttl = lifeTime;
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

    public int getTtl() {
        initTtl();
        return ttl;
    }

    private void initTtl() {
        if (ttl > 0) {
            return;
        }
        ttl = ServerUtil.calculateTtl(creationDate, expirationDate);
        if (ttl > 0) {
            return;
        }
        // unable to calculate ttl (expiration or creation date is not set), thus defaults it to 1 day
        ttl = (int) TimeUnit.DAYS.toSeconds(1);
    }

    public void resetTtlFromExpirationDate() {
        final Long duration = Duration.between(new Date().toInstant(), getExpirationDate().toInstant()).getSeconds();
        final Integer seconds = duration.intValue();
        if (seconds != null) {
            this.ttl = seconds;
        }
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
    public synchronized boolean isRevoked() {
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
    public synchronized boolean isExpired() {
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

    public String getX5ts256() {
        return x5ts256;
    }

    public void setX5ts256(String x5ts256) {
        this.x5ts256 = x5ts256;
    }

    public String getDpop() {
        return dpop;
    }

    public void setDpop(String dpop) {
        this.dpop = dpop;
    }

    public String getSessionDn() {
        return sessionDn;
    }

    public void setSessionDn(String sessionDn) {
        this.sessionDn = sessionDn;
    }

    @Override
    public Boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
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
        return HashUtil.getHash(input, signatureAlgorithm);
    }
}