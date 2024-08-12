/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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