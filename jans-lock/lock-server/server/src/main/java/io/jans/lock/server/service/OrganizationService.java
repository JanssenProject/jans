/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.server.service;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.model.ApplicationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides operations with organization
 *
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
public class OrganizationService extends io.jans.service.OrganizationService {

	private static final long serialVersionUID = 4502134792415981865L;

	@Inject
    private AppConfiguration appConfiguration;

	public String getDnForOrganization() {
		return getDnForOrganization(appConfiguration.getBaseDN());
	}

	/**
	 * Build DN string for organization
	 * 
	 * @return DN string for organization
	 */
	public String getBaseDn() {
		return appConfiguration.getBaseDN();
	}

	@Override
	public ApplicationType getApplicationType() {
		return ApplicationType.JANS_LOCK;
	}

}
