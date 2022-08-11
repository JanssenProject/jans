/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;


import io.jans.as.model.config.StaticConfiguration;

import io.jans.orm.PersistenceEntryManager;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;


@ApplicationScoped
public class SessionService {

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private Logger logger;

    
}
