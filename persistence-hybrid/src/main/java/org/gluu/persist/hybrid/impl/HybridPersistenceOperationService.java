package org.gluu.persist.hybrid.impl;

import java.util.List;

import org.gluu.persist.operation.PersistenceOperationService;

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

}
