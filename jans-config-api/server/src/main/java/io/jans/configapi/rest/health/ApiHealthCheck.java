/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.health;

import io.jans.configapi.model.status.StatsData;
import io.jans.configapi.rest.resource.auth.ConfigBaseResource;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiConstants;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.json.JSONArray;

import org.slf4j.Logger;

@Path(ApiConstants.HEALTH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiHealthCheck extends ConfigBaseResource {
    
    @Inject
    Logger logger;

    private static final String STATUS = "status";
        
    @Inject
    ConfigurationService configurationService;

    @GET
    public Response getHealthResponse() {
        logger.debug("Api Health Check - Entry");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(STATUS, "UP");

        // liveness
        JSONObject dataJsonObject = new JSONObject();
        dataJsonObject.put("name", "jans-config-api liveness");
        dataJsonObject.put(STATUS, "UP");
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(0, dataJsonObject);

        // readiness
        dataJsonObject = new JSONObject();
        dataJsonObject.put("name", "jans-config-api readiness");
        try {
            checkDatabaseConnection();
            dataJsonObject.put(STATUS, "UP");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            dataJsonObject.put(STATUS, "DOWN");
            dataJsonObject.put("error", "e.getMessage()");
            logger.debug("Api Health Check - Error - jsonObject:{}",jsonObject);
        }
        jsonArray.put(1, dataJsonObject);

        jsonObject.put("checks", jsonArray);
        logger.debug("ApiHealthCheck::getHealthResponse() - jsonObject:{}",jsonObject);
        return Response.ok(jsonObject.toString()).build();
    }

    @GET
    @Path(ApiConstants.LIVE)
    public Response getLivenessResponse() {
        logger.debug("ApiHealthCheck::getLivenessResponse() - Entry");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "jans-config-api liveness");
        jsonObject.put(STATUS, "UP");
        logger.debug("ApiHealthCheck::getLivenessResponse() - jsonObject:{}",jsonObject);
        return Response.ok(jsonObject.toString()).build();
    }

    @GET
    @Path(ApiConstants.READY)
    public Response getReadinessResponse() {
        logger.debug("ApiHealthCheck::getReadinessResponse() - Entry");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "jans-config-api readiness");
        try {
            checkDatabaseConnection();
            jsonObject.put(STATUS, "UP");
            logger.debug("Api Health Readiness - Success - jsonObject:{}",jsonObject);
            return Response.ok(jsonObject.toString()).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            jsonObject.put("error", "e.getMessage()");
            logger.debug("Api Health Readiness - Error - jsonObject:{}",jsonObject);
            return Response.ok(jsonObject.toString()).build();
        }
    }

    @GET
    @Path(ApiConstants.SERVER_STAT)
    public Response getServerStat() {
        logger.debug("Server Stat - Entry");
        StatsData statsData = configurationService.getStatsData();
        logger.debug("Server Stat - statsData:{}",statsData);
        return Response.ok(statsData).build();

    }

    private void checkDatabaseConnection() {
        configurationService.findConf();
    }
}
