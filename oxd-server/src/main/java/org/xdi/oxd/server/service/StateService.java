package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.server.Configuration;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/09/2016
 */

public class StateService {

    private static final Logger LOG = LoggerFactory.getLogger(StateService.class);

    private final Cache<String, String> states;
    private final Cache<String, String> nonces;

    private final SecureRandom random = new SecureRandom();

    @Inject
    ConfigurationService configurationService;

    public StateService() {

        Configuration conf = configurationService.get();

        states = CacheBuilder.newBuilder()
                .expireAfterWrite(conf.getStateExpirationInMinutes(), TimeUnit.MINUTES)
                .build();
        nonces = CacheBuilder.newBuilder()
                .expireAfterWrite(conf.getNonceExpirationInMinutes(), TimeUnit.MINUTES)
                .build();

    }

    public String generateState() {
        String state = generateSecureString();
        putState(state);
        return state;
    }

    public String generateNonce() {
        String nonce = generateSecureString();
        nonces.put(nonce, nonce);
        return nonce;
    }

    private String generateSecureString() {
        return new BigInteger(130, random).toString(32);
    }

    public boolean isStateValid(String state) {
        return !Strings.isNullOrEmpty(states.getIfPresent(state));
    }

    public void invalidateState(String state) {
        states.invalidate(state);
    }

    public void putState(String state) {
        states.put(state, state);
    }
}
