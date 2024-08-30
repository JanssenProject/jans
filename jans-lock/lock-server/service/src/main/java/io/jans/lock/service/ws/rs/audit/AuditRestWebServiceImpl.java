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

package io.jans.lock.service.ws.rs.audit;

import io.jans.lock.service.audit.AuditService;
import io.jans.lock.util.ServerUtil;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Provides interface for audit REST web services
 *  
 * @author Yuriy Movchan Date: 06/06/2024
 */
@Dependent
@Path("/audit")
public class AuditRestWebServiceImpl implements AuditRestWebService {

    @Inject
    private Logger log;

    @Inject
    AuditService auditService;

    @Override
    public Response processHealthRequest(HttpServletRequest request, HttpServletResponse response,
            SecurityContext sec) {
        log.debug("Processing Health request - request:{}", request);
        return processAuditRequest(request, "Health");
    }

    @Override
    public Response processLogRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.debug("Processing Log request - request:{}", request);
        return processAuditRequest(request, "log");

    }

    @Override
    public Response processTelemetryRequest(HttpServletRequest request, HttpServletResponse response,
            SecurityContext sec) {
        log.debug("Processing Telemetry request - request:{}", request);
        return processAuditRequest(request, "telemetry");

    }

    private Response processAuditRequest(HttpServletRequest request, String requestType) {
        log.debug("Processing request - request:{}, requestType:{}", request, requestType);

        Response.ResponseBuilder builder = Response.ok();
        builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
        builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);

        JSONObject json = this.auditService.getJSONObject(request);
        Response response = this.auditService.post(requestType, json.toString(), ContentType.APPLICATION_JSON);
        log.debug("response:{}", response);

        if (response != null) {
            log.trace(
                    "Response for Access Token -  response.getStatus():{}, response.getStatusInfo():{}, response.getEntity().getClass():{}",
                    response.getStatus(), response.getStatusInfo(), response.getEntity().getClass());
            String entity = response.readEntity(String.class);
            log.debug(" entity:{}", entity);
            builder.entity(entity);

            if (response.getStatusInfo().equals(Status.OK)) {

                log.debug(" Status.CREATED:{}, entity:{}", Status.OK, entity);
            } else {
                log.error("Error while saving audit data - response.getStatusInfo():{}, entity:{}",
                        response.getStatusInfo(), entity);
                builder.status(response.getStatusInfo());
            }
        }

        return builder.build();
    }

}
