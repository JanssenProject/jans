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

import io.jans.lock.model.config.StaticConfiguration;
import io.jans.service.BaseCacheService;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
public class AttributeService extends io.jans.service.AttributeService {

	@Inject
	private StaticConfiguration staticConfiguration;

    @Inject
    protected CacheService cacheService;

    @Inject
    protected LocalCacheService localCacheService;

    protected boolean isUseLocalCache() {
    	return true;
    }

	@Override
	protected BaseCacheService getCacheService() {
        if (isUseLocalCache()) {
            return localCacheService;
        }
        return cacheService;
	}

	@Override
    public String getDnForAttribute(String inum) {
        String attributesDn = staticConfiguration.getBaseDn().getAttributes();
        if (StringHelper.isEmpty(inum)) {
            return attributesDn;
        }
        return String.format("inum=%s,%s", inum, attributesDn);
    }

}