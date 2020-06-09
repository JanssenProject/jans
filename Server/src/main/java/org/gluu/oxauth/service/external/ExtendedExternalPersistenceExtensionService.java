/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.oxauth.service.external;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.service.common.ApplicationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.extension.PersistenceExtension;
import org.gluu.service.external.ExternalPersistenceExtensionService;
import org.slf4j.Logger;

/**
 * Provides factory methods needed to create persistence extension
 *
 * @author Yuriy Movchan Date: 06/08/2020
 */
@ApplicationScoped
public class ExtendedExternalPersistenceExtensionService  extends ExternalPersistenceExtensionService {

	private static final long serialVersionUID = 7257467958916183397L;

    @Inject
	@Named(ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME)
	private List<PersistenceEntryManager> persistenceAuthEntryManagers;

	@Override
	protected void reloadExternal() {
		super.reloadExternal();

		PersistenceExtension persistenceExtension = null;
		if (isEnabled()) {
			persistenceExtension = (PersistenceExtension) this.defaultExternalCustomScript.getExternalType();
		}
		
		for (PersistenceEntryManager persistenceAuthEntryManager : persistenceAuthEntryManagers) {
			persistenceAuthEntryManager.setPersistenceExtension(persistenceExtension);
		}
    }

}
