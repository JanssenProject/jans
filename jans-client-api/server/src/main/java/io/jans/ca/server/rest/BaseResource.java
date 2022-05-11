package io.jans.ca.server.rest;

import io.jans.as.model.util.Util;
import io.jans.ca.common.Command;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.HasRpIdParams;
import io.jans.ca.common.params.IParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Processor;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.RpSyncService;
import io.jans.ca.server.service.ValidationService;
import io.jans.ca.server.persistence.service.JansConfigurationService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class BaseResource {

    @Inject
    Logger logger;

    @Inject
    JansConfigurationService jansConfigurationService;
    @Inject
    RpSyncService rpSyncService;
    @Inject
    ValidationService validationService;
    @Inject
    Processor processor;

    @Context
    private HttpServletRequest httpRequest;
    private static final String LOCALHOST_IP_ADDRESS = "127.0.0.1";

    public <T> T read(String params, Class<T> clazz) {
        try {
            return Jackson2.createJsonMapper().readValue(params, clazz);
        } catch (IOException e) {
            logger.error("Invalid params: " + params + " exception: {}", e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid parameters. Message: " + e.getMessage()).build());
        }
    }

    public <T extends IParams> String process(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization, String AuthorizationRpId) {
        logger.info("Endpoint: {}", httpRequest.getRequestURL().toString());
        logger.info("Request parameters: {}", paramsAsString);
        logger.info("CommandType: {}", commandType);

        validateIpAddressAllowed(httpRequest.getRemoteAddr());
        Object forJsonConversion = getObjectForJsonConversion(commandType, paramsAsString, paramsClass, authorization, AuthorizationRpId);
        String response = null;

        if (commandType.getReturnType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
            response = Jackson2.asJsonSilently(forJsonConversion);
        } else if (commandType.getReturnType().equalsIgnoreCase(MediaType.TEXT_PLAIN)) {
            response = forJsonConversion.toString();
        }

        logger.info("Send back response: {}", response);
        logger.trace("Send back response: {}", response);
        return response;
    }

    private void validateIpAddressAllowed(String callerIpAddress) {
        logger.trace("Checking if caller ipAddress : {} is allowed to make request to jans_client_api.", callerIpAddress);
        logger.info("Checking if caller ipAddress : {} is allowed to make request to jans_client_api.", callerIpAddress);
        final ApiAppConfiguration conf = jansConfigurationService.find();
        List<String> bindIpAddresses = conf.getBindIpAddresses();

        //localhost as default bindAddress
        if ((bindIpAddresses == null || bindIpAddresses.isEmpty()) && LOCALHOST_IP_ADDRESS.equalsIgnoreCase(callerIpAddress)) {
            return;
        }
        //show error if ip_address of a remote caller is not set in `bind_ip_addresses`
        if (bindIpAddresses == null || bindIpAddresses.isEmpty()) {
            logger.error("The caller is not allowed to make request to jans_client_api. To allow add ip_address of caller in `bind_ip_addresses` array of configuration.");
            throw new HttpException(ErrorResponseCode.RP_ACCESS_DENIED);
        }
        //allow all ip_address
        if (bindIpAddresses.contains("*")) {
            return;
        }

        if (bindIpAddresses.contains(callerIpAddress)) {
            return;
        }
        logger.error("The caller is not allowed to make request to jans_client_api. To allow add ip_address of caller in `bind_ip_addresses` array of configuration.");
        throw new HttpException(ErrorResponseCode.RP_ACCESS_DENIED);
    }

    private <T extends IParams> Object getObjectForJsonConversion(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization, String AuthorizationRpId) {
        logger.trace("Command: {}", paramsAsString);
        T params = read(safeToJson(paramsAsString), paramsClass);

        final ApiAppConfiguration conf = jansConfigurationService.find();

        if (commandType.isAuthorizationRequired()) {
            validateAuthorizationRpId(conf, AuthorizationRpId);
            validateAccessToken(authorization, safeToRpId((HasRpIdParams) params, AuthorizationRpId));
        }

        Command command = new Command(commandType, params);
        final IOpResponse response = processor.process(command);
        Object forJsonConversion = response;
        if (response instanceof POJOResponse) {
            forJsonConversion = ((POJOResponse) response).getNode();
        }
        return forJsonConversion;
    }

    private void validateAuthorizationRpId(ApiAppConfiguration conf, String AuthorizationRpId) {

        if (Util.isNullOrEmpty(AuthorizationRpId)) {
            return;
        }

        final Rp rp = rpSyncService.getRp(AuthorizationRpId);

        if (rp == null || Util.isNullOrEmpty(rp.getRpId())) {
            logger.debug("`rp_id` in `AuthorizationRpId` header is not registered in jans_client_api.");
            throw new HttpException(ErrorResponseCode.AUTHORIZATION_RP_ID_NOT_FOUND);
        }

        if (conf.getProtectCommandsWithRpId() == null || conf.getProtectCommandsWithRpId().isEmpty()) {
            return;
        }

        if (!conf.getProtectCommandsWithRpId().contains(AuthorizationRpId)) {
            logger.debug("`rp_id` in `AuthorizationRpId` header is invalid. The `AuthorizationRpId` header should contain `rp_id` from `protect_commands_with_rp_id` field in client-api-server.yml.");
            throw new HttpException(ErrorResponseCode.INVALID_AUTHORIZATION_RP_ID);
        }
    }

    private void validateAccessToken(String authorization, String AuthorizationRpId) {
        final String prefix = "Bearer ";
        final ApiAppConfiguration conf = jansConfigurationService.find();

        if (conf.getProtectCommandsWithAccessToken() != null && !conf.getProtectCommandsWithAccessToken()) {
            logger.debug("Skip protection because protect_commands_with_access_token: false in configuration file.");
            return;
        }

        if (Util.isNullOrEmpty(authorization)) {
            logger.debug("No access token provided in Authorization header. Forbidden.");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        String accessToken = authorization.substring(prefix.length());
        if (Util.isNullOrEmpty(accessToken)) {
            logger.debug("No access token provided in Authorization header. Forbidden.");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        validationService.validateAccessToken(accessToken, AuthorizationRpId);
    }

    private String safeToRpId(HasRpIdParams params, String AuthorizationRpId) {
        return Util.isNullOrEmpty(AuthorizationRpId) ? params.getRpId() : AuthorizationRpId;
    }

    private String safeToJson(String jsonString) {
        return Util.isNullOrEmpty(jsonString) ? "{}" : jsonString;
    }
}
