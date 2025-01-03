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

package io.jans.lock.service.util;

import java.util.HashSet;
import java.util.Set;

import io.jans.lock.service.filter.AuthorizationProcessingFilter;
import io.jans.lock.service.ws.rs.ConfigurationRestWebService;
import io.jans.lock.service.ws.rs.audit.AuditRestWebServiceImpl;
import io.jans.lock.service.ws.rs.config.ConfigRestWebServiceImpl;
import io.jans.lock.service.ws.rs.sse.SseRestWebServiceImpl;
import io.jans.lock.service.ws.rs.stat.StatRestWebServiceImpl;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Integration with Resteasy
 * 
 * @author Yuriy Movchan Date: 06/06/2024
 */
@ApplicationPath("/v1")
public class ResteasyInitializer extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(ConfigurationRestWebService.class);

		classes.add(AuditRestWebServiceImpl.class);
		classes.add(ConfigRestWebServiceImpl.class);
		classes.add(StatRestWebServiceImpl.class);

		classes.add(SseRestWebServiceImpl.class);

		classes.add(AuthorizationProcessingFilter.class);

		return classes;
	}

}
