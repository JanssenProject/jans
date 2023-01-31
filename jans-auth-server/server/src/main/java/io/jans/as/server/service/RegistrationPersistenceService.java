/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.slf4j.Logger;
import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Every registration is persisted under Person Entry
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class RegistrationPersistenceService extends io.jans.as.common.service.common.fido2.RegistrationPersistenceService {

	
    
    public String getUserInum(String userName)
    {
    	return userService.getUserInum(userName);
    }

}
