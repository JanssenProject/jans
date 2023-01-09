/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.shared;

import java.util.List;

import io.jans.fido2.model.conf.AppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.config.StaticConfiguration;

@ApplicationScoped
public class UserService extends io.jans.as.common.service.common.UserService {

	public static final String[] USER_OBJECT_CLASSES = new String[] { AttributeConstants.JANS_PERSON };

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Override
	public List<String> getPersonCustomObjectClassList() {
		return appConfiguration.getPersonCustomObjectClassList();
	}

    @Override
    public String getPeopleBaseDn() {
		return staticConfiguration.getBaseDn().getPeople();
	}

}
