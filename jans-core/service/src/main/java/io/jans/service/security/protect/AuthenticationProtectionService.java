/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.security.protect;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import io.jans.service.CacheService;
import io.jans.model.security.protect.AuthenticationAttempt;
import io.jans.model.security.protect.AuthenticationAttemptList;
import org.slf4j.Logger;

/**
 * Base Brute Force authentication protection service implementation
 *
 * @author Yuriy Movchan Date: 08/21/2018
 */
public abstract class AuthenticationProtectionService {

    @Inject
    private CacheService cacheService;

    @Inject
    private Logger log;

    protected int attemptExpiration;
    protected int maximumAllowedAttemptsWithoutDelay;

    protected int delayTime;

    @PostConstruct
    public void create() {
        init();
    }

    protected abstract void init();

    public void storeAttempt(String key, boolean success) {
        AuthenticationAttemptList authenticationAttemptList = getNonExpiredAttempts(key);
        if (authenticationAttemptList == null) {
            authenticationAttemptList = new AuthenticationAttemptList();
        }

        long currentTime = System.currentTimeMillis();

        AuthenticationAttempt authenticationAttempt = new AuthenticationAttempt(currentTime, currentTime + attemptExpiration * 1000, success);
        authenticationAttemptList.getAuthenticationAttempts().add(authenticationAttempt);

        cacheService.put(Integer.toString(attemptExpiration), buildKey(key), authenticationAttemptList);
    }

    public AuthenticationAttemptList getAttempts(String key) {
        Object o = cacheService.get(null, buildKey(key));
        if (o instanceof AuthenticationAttemptList) {
            return (AuthenticationAttemptList) o;
        }

        return null;
    }

    public AuthenticationAttemptList getNonExpiredAttempts(String key) {
        AuthenticationAttemptList authenticationAttemptList = getAttempts(key);
        if (authenticationAttemptList == null) {
            return null;
        }

        long currentTime = System.currentTimeMillis();

        AuthenticationAttemptList result = new AuthenticationAttemptList();
        List<AuthenticationAttempt> resultAuthenticationAttemptList = result.getAuthenticationAttempts();
        for (AuthenticationAttempt authenticationAttempt : authenticationAttemptList.getAuthenticationAttempts()) {
            if (authenticationAttempt.getExpiration() > currentTime) {
                resultAuthenticationAttemptList.add(authenticationAttempt);
            }
        }

        return result;

    }

    public boolean isReachAttemptRateLimit(String key) {
        AuthenticationAttemptList authenticationAttemptList = getNonExpiredAttempts(key);
        if (authenticationAttemptList == null) {
            return false;
        }

        return authenticationAttemptList.getAuthenticationAttempts().size() >= maximumAllowedAttemptsWithoutDelay;
    }

    public int getDelayTime() {
        return delayTime;
    }
    
    public void doDelayIfNeeded(String key) {
        boolean processDelay = isReachAttemptRateLimit(key);
        if (!processDelay) {
            log.debug("Allowing current login attempt without delay");
            return;
        }
        
        int delayTime = getDelayTime();
        
        try {
            log.debug("Current login attempt requires delay: '{}' seconds", delayTime);
            Thread.sleep(delayTime * 1000);
        } catch (InterruptedException ex) {
            log.error("Failed to process authentication delay");
        }
    }

    private String buildKey(String key) {
        return getKeyPrefix() + "_" + key.toLowerCase();
    }

    protected abstract String getKeyPrefix();

}
