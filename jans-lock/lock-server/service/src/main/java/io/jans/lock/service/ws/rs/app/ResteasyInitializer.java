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

package io.jans.lock.service.ws.rs.app;

import java.util.HashSet;
import java.util.Set;

import io.jans.lock.service.filter.AuthorizationProcessingFilter;
import io.jans.lock.service.ws.rs.ConfigurationRestWebService;
import io.jans.lock.service.ws.rs.audit.AuditRestWebServiceImpl;
import io.jans.lock.service.ws.rs.config.ConfigRestWebServiceImpl;
import io.jans.lock.service.ws.rs.sse.SseRestWebServiceImpl;
import io.jans.lock.service.ws.rs.stat.StatRestWebServiceImpl;
import io.jans.lock.util.ApiAccessConstants;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.servers.*;

/**
 * Integration with Resteasy
 * 
 * @author Yuriy Movchan Date: 06/06/2024
 */
@ApplicationPath("/api/v1")
@OpenAPIDefinition(info = @Info(title = "Jans Lock API", contact =
@Contact(name = "Contact", url = "https://github.com/JanssenProject/jans/discussions"),

        license = @License(name = "License", url = "https://github" +
                ".com/JanssenProject/jans/blob/main/LICENSE"),

        version = "OAS Version"),

        tags = { @Tag(name = "Lock"), @Tag(name = "Cedarling"),
                @Tag(name = "Audit Log"), @Tag(name = "Audit Health"),
                @Tag(name = "Audit Telemetry"), @Tag(name = "SSE broadcast"),
                @Tag(name = "Token Status List"), @Tag(name = "Events"),
                @Tag(name = "Statistics MAU"), @Tag(name = "Statistics MAC"),
                @Tag(name = "Config API Lock Plugin"), @Tag(name = "Custom Scripts"),
                @Tag(name = "Token"),
        },

        servers = { @Server(url = "https://jans.local.io", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
        @OAuthScope(name = ApiAccessConstants.LOCK_CONFIG_READ_ACCESS, description = "View configuration related information"),
        @OAuthScope(name = ApiAccessConstants.LOCK_CONFIG_ISSUERS_READ_ACCESS, description = "View issuers related information"),
        @OAuthScope(name = ApiAccessConstants.LOCK_CONFIG_SCHEMA_READ_ACCESS, description = "View schema related information"),
        @OAuthScope(name = ApiAccessConstants.LOCK_CONFIG_POLICY_READ_ACCESS, description = "View policy related information"),
        @OAuthScope(name = ApiAccessConstants.LOCK_HEALTH_WRITE_ACCESS, description = "Write audit health entries"),
        @OAuthScope(name = ApiAccessConstants.LOCK_LOG_WRITE_ACCESS, description = "Write audit log entries"),
        @OAuthScope(name = ApiAccessConstants.LOCK_TELEMETRY_WRITE_ACCESS, description = "Write telemetry health entries"),
        @OAuthScope(name = ApiAccessConstants.LOCK_SSE_READ_ACCESS, description = "Subscribe to SSE events"),
        @OAuthScope(name = ApiAccessConstants.LOCK_STAT_READ_ACCESS, description = "View stat related information")
        }

)))
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
