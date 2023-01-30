/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common.fido2;

/**
 * Abstract class for registrations that are persisted under Person Entry
 * @author madhumitas
 *
 */

public abstract class RegistrationPersistenceService {

	public abstract String getUserInum(String userName);

}
