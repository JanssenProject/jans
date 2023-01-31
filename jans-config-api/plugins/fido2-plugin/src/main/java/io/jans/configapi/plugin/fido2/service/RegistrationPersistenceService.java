/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import io.jans.as.common.service.common.UserService;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.enterprise.context.ApplicationScoped;
/**
 * Override RegistrationPersistenceService bean
 * @author Yuriy Movchan
 * @version Jun 01, 2023
 */
@ApplicationScoped
public class RegistrationPersistenceService extends io.jans.as.common.service.common.fido2.RegistrationPersistenceService {

    public String getUserInum(String userName)
    {
    	return userService.getUserInum(userName);
    }
    
}
