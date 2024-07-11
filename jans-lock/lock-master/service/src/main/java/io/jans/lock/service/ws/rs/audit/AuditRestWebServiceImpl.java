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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.lock.service.util.ServerUtil;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.HashMap;
import io.jans.lock.service.util.AuthUtil;

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
	AuthUtil authUtil;

	@Override
	public Response processHealthRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Health request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");

		return builder.build();
	}

	@Override
	public Response processLogRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) 
	{
		log.debug("Processing Log request - request:{}",request);
		
		JSONObject jsonBody = getJSONObject(request);
		Response.ResponseBuilder builder = Response.ok();
		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");
		//return builder.build();
		
		return Response.status(Response.Status.OK).entity(jsonBody).build();
	}
	

	@Override
	public Response processTelemetryRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.error("Processing Telemetry request - request:{}", request);
		Response.ResponseBuilder builder = Response.ok();

		this.authUtil.getAppConfiguration();
		
		this.postData(this.getJSONObject(request));
		return builder.build();
	}
	
	private void postData(JSONObject json) {
	    log.error("Processing Telemetry request - json:{}", json);
	    if(json==null) {
	        return;
	    }
	    log.error("Processing Telemetry request");
	    this.authUtil.postData(json.toString());
	   
	    
	}
	
	   private JSONObject getJSONObject(HttpServletRequest request) {
	        JSONObject jsonBody = null;
	        if(request==null) {
	            return jsonBody;
	        }
	        try {
	            String jsonBodyStr = IOUtils.toString(request.getInputStream());
	            log.error(" jsonBodyStr:{}",jsonBodyStr);
	            jsonBody = new JSONObject(jsonBodyStr);
	            log.error(" jsonBody:{}",jsonBody);
	        }catch(Exception ex) {
	            ex.printStackTrace();
	            log.error("Exception while retriving json from request is :{}",ex );
	        }
	        return jsonBody;
	    }

	

	
	

}
