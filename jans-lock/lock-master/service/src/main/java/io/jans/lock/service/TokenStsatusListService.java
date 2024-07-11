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

package io.jans.lock.service;

import org.slf4j.Logger;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.lock.model.config.StaticConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 
 * Token service
 *
 * @author Yuriy Movchan Date: 01/05/2024
 */
@ApplicationScoped
public class TokenStsatusListService {

    public static final String CONTENT_TYPE_STATUSLIST_JSON = "application/statuslist+json";
    public static final String CONTENT_TYPE_STATUSLIST_JWT = "application/statuslist+jwt";

    @Inject
    private Logger log;

    @Inject
    private OpenIdService openIdService;

    @Inject
    private StaticConfiguration staticConfiguration;

    public Jwt loadTokenStatusList() {
    	OpenIdConfigurationResponse openIdConfiguration = openIdService.getOpenIdConfiguration();
    	System.out.println(openIdConfiguration);
    	return null;
    }

}