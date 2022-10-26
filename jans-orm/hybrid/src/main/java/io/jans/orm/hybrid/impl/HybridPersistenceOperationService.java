/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.hybrid.impl;

import java.util.List;

import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.extension.PersistenceExtension;
import io.jans.orm.operation.PersistenceOperationService;

/**
 * Hybrid Operation Service
 *
 * @author Yuriy Movchan Date: 05/13/2018
 */
public class HybridPersistenceOperationService implements PersistenceOperationService {

	private List<PersistenceOperationService> persistenceOperationServices;

	public HybridPersistenceOperationService(List<PersistenceOperationService> persistenceOperationServices) {
		this.persistenceOperationServices = persistenceOperationServices;
	}

	@Override
	public boolean isConnected() {
		for(PersistenceOperationService persistenceOperationService : persistenceOperationServices) {
			if (!persistenceOperationService.isConnected()) {
				return false;
			}
		}

		return true;
	}

	public List<PersistenceOperationService> getPersistenceOperationServices() {
		return persistenceOperationServices;
	}

	@Override
	public void setPersistenceExtension(PersistenceExtension persistenceExtension) {
		for(PersistenceOperationService persistenceOperationService : persistenceOperationServices) {
			persistenceOperationService.setPersistenceExtension(persistenceExtension);
		}
	}

	@Override
	public boolean authenticate(String key, String password, String objectClass) throws ConnectionException, SearchException, AuthenticationException {
        throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public boolean isSupportObjectClass(String objectClass) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

}
