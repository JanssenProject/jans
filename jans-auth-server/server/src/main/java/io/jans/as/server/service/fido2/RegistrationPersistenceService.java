/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.fido2;

import org.slf4j.Logger;

import io.jans.as.common.service.common.fido2.BaseRegistrationPersistenceService;
import io.jans.as.model.config.StaticConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Every registration is persisted under Person Entry
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class RegistrationPersistenceService extends BaseRegistrationPersistenceService {

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    public String getBasedPeopleDn() {
    	return staticConfiguration.getBaseDn().getPeople();
    }

}
