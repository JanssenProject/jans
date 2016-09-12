package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/09/2016
 */

public class StateService {

    private static final Logger LOG = LoggerFactory.getLogger(StateService.class);

    private final Cache<String, String> states = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();
    private final Cache<String, String> nonces = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    private final SecureRandom random = new SecureRandom();

    public StateService() {
    }

    public String generateState() {
        String state = generateSecureString();
        states.put(state, state);
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
}
