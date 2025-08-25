/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.AttributeType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import java.util.*;

@ApplicationScoped
public class DatabaseService {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    public Map<String, Map<String, AttributeType>> getTableColumnsMap() {
        return persistenceEntryManager.getTableColumnsMap();
    }    
   
}
