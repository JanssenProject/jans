/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.service;

import org.slf4j.Logger;

import io.jans.lock.model.config.StaticConfiguration;
import io.jans.model.token.TokenEntity;
import io.jans.orm.PersistenceEntryManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 
 * Token service
 *
 * @author Yuriy Movchan Date: 01/05/2024
 */
@ApplicationScoped
public class TokenService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    public String buildDn(String hashedToken) {
        return String.format("tknCde=%s,", hashedToken) + tokenBaseDn();
    }

    private String tokenBaseDn() {
        return staticConfiguration.getBaseDn().getTokens();  // ou=tokens,o=jans
    }

    public TokenEntity findToken(String tokenCode) {
    	String tokenDn = buildDn(tokenCode);
        return persistenceEntryManager.find(TokenEntity.class, tokenDn);
    }

}