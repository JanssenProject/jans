/*
  All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

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
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import io.jans.ca.server.service.RpSyncService;
import io.jans.ca.server.service.ValidationService;
import io.jans.ca.server.utils.Convertor;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public abstract class TemplateOperation<T extends IParams> implements ITemplateOperation<T> {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateOperation.class);
    private static final String LOCALHOST_IP_ADDRESS = "127.0.0.1";

    @Inject
    ValidationService validationService;
    @Inject
    RpSyncService rpSyncService;
    @Inject
    MainPersistenceService jansConfigurationService;

    public Response process(String paramsAsString, HttpServletRequest httpRequest) {
        String endPointUrl = httpRequest.getRequestURL().toString();
        LOG.info("Endpoint: {}", endPointUrl);
        LOG.info("Request parameters: {}", paramsAsString);
        LOG.info("CommandType: {}", getCommandType());

        validateIpAddressAllowed(httpRequest.getRemoteAddr());
        Object forJsonConversion = getObjectForJsonConversion(paramsAsString, getParameterClass(), httpRequest);
        String response = null;

        if (getCommandType().getReturnType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
            response = Jackson2.asJsonSilently(forJsonConversion);
        } else if (getCommandType().getReturnType().equalsIgnoreCase(MediaType.TEXT_PLAIN)) {
            response = forJsonConversion.toString();
        }

        LOG.trace("Send back response: {}", response);
        return Response.ok(response).build();
    }

    public Response process(String paramsAsString, String authorization, String authorizationRpId, HttpServletRequest httpRequest) {
        String endPointUrl = httpRequest.getRequestURL().toString();
        LOG.info("Endpoint: {}", endPointUrl);
        LOG.info("Request parameters: {}", paramsAsString);
        LOG.info("CommandType: {}", getCommandType());

        validateIpAddressAllowed(httpRequest.getRemoteAddr());
        Object forJsonConversion = getObjectForJsonConversion(paramsAsString, getParameterClass(), authorization, authorizationRpId, httpRequest);
        String response = null;

        if (getCommandType().getReturnType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
            response = Jackson2.asJsonSilently(forJsonConversion);
        } else if (getCommandType().getReturnType().equalsIgnoreCase(MediaType.TEXT_PLAIN)) {
            response = forJsonConversion.toString();
        }

        LOG.trace("Send back response: {}", response);
        return Response.ok(response).build();
    }

    private void validateIpAddressAllowed(String callerIpAddress) {
        LOG.trace("Checking if caller ipAddress : {} is allowed to make request to jans_client_api.", callerIpAddress);
        final ApiAppConfiguration conf = jansConfigurationService.find();
        List<String> bindIpAddresses = conf.getBindIpAddresses();

        //localhost as default bindAddress
        if ((bindIpAddresses == null || bindIpAddresses.isEmpty()) && LOCALHOST_IP_ADDRESS.equalsIgnoreCase(callerIpAddress)) {
            return;
        }
        //show error if ip_address of a remote caller is not set in `bind_ip_addresses`
        if (bindIpAddresses == null || bindIpAddresses.isEmpty()) {
            LOG.error("The caller is not allowed to make request to jans_client_api. To allow add ip_address of caller in `bind_ip_addresses` array of configuration.");
            throw new HttpException(ErrorResponseCode.RP_ACCESS_DENIED);
        }
        //allow all ip_address
        if (bindIpAddresses.contains("*")) {
            return;
        }

        if (bindIpAddresses.contains(callerIpAddress)) {
            return;
        }
        LOG.error("The caller is not allowed to make request to jans_client_api. To allow add ip_address of caller in `bind_ip_addresses` array of configuration.");
        throw new HttpException(ErrorResponseCode.RP_ACCESS_DENIED);
    }

    private <T extends IParams> Object getObjectForJsonConversion(String paramsAsString, Class<T> paramsClass, HttpServletRequest httpRequest) {
        LOG.trace("Command: {}", paramsAsString);
        T params = read(safeToJson(paramsAsString), paramsClass);
        Command command = new Command(getCommandType(), params);
        final IOpResponse response = internProcess(command, httpRequest);
        Object forJsonConversion = response;
        if (response instanceof POJOResponse) {
            forJsonConversion = ((POJOResponse) response).getNode();
        }
        return forJsonConversion;
    }

    private <T extends IParams> Object getObjectForJsonConversion(String paramsAsString, Class<T> paramsClass, String authorization, String authorizationRpId, HttpServletRequest httpRequest) {
        LOG.trace("Command: {}", paramsAsString);
        T params = read(safeToJson(paramsAsString), paramsClass);

        final ApiAppConfiguration conf = jansConfigurationService.find();

        if (getCommandType().isAuthorizationRequired()) {
            validateAuthorizationRpId(conf, authorizationRpId);
            validateAccessToken(authorization, safeToRpId((HasRpIdParams) params, authorizationRpId));
        }

        Command command = new Command(getCommandType(), params);
        final IOpResponse response = internProcess(command, httpRequest);
        Object forJsonConversion = response;
        if (response instanceof POJOResponse) {
            forJsonConversion = ((POJOResponse) response).getNode();
        }
        return forJsonConversion;
    }


    private IOpResponse internProcess(Command command, HttpServletRequest httpRequest) {
        try {
            IParams iParams = Convertor.asParams(getParameterClass(), command);
            validationService.validate(iParams);

            IOpResponse operationResponse = execute((T) iParams, httpRequest);
            if (operationResponse != null) {
                return operationResponse;
            } else {
                LOG.error("No response from operation. Command: {}", getCommandType().getValue());
            }
        } catch (ClientErrorException e) {
            throw new WebApplicationException(e.getResponse().readEntity(String.class), e.getResponse().getStatus());
        } catch (WebApplicationException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw e;
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        throw HttpException.internalError();
    }

    public <T> T read(String params, Class<T> clazz) {
        try {
            return Jackson2.createJsonMapper().readValue(params, clazz);
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid parameters. Message: " + e.getMessage()).build());
        }
    }

    private String safeToJson(String jsonString) {
        return Util.isNullOrEmpty(jsonString) ? "{}" : jsonString;
    }

    public Rp getRp(T params) {
        if (params instanceof HasRpIdParams) {
            validationService.validate((HasRpIdParams) params);
            HasRpIdParams hasRpId = (HasRpIdParams) params;
            return rpSyncService.getRp(hasRpId.getRpId());
        }
        throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_RP_ID);
    }

    private String safeToRpId(HasRpIdParams params, String authorizationRpId) {
        return Util.isNullOrEmpty(authorizationRpId) ? params.getRpId() : authorizationRpId;
    }

    private void validateAuthorizationRpId(ApiAppConfiguration conf, String authorizationRpId) {

        if (Util.isNullOrEmpty(authorizationRpId)) {
            return;
        }

        final Rp rp = rpSyncService.getRp(authorizationRpId);

        if (rp == null || Util.isNullOrEmpty(rp.getRpId())) {
            LOG.debug("`rp_id` in `AuthorizationRpId` header is not registered in jans_client_api.");
            throw new HttpException(ErrorResponseCode.AUTHORIZATION_RP_ID_NOT_FOUND);
        }

        if (conf.getProtectCommandsWithRpId() == null || conf.getProtectCommandsWithRpId().isEmpty()) {
            return;
        }

        if (!conf.getProtectCommandsWithRpId().contains(authorizationRpId)) {
            LOG.debug("`rp_id` in `AuthorizationRpId` header is invalid. The `AuthorizationRpId` header should contain `rp_id` from `protect_commands_with_rp_id` field in client-api-server.yml.");
            throw new HttpException(ErrorResponseCode.INVALID_AUTHORIZATION_RP_ID);
        }
    }

    private void validateAccessToken(String authorization, String authorizationRpId) {
        final String prefix = "Bearer ";
        final ApiAppConfiguration conf = jansConfigurationService.find();

        if (conf.getProtectCommandsWithAccessToken() != null && !conf.getProtectCommandsWithAccessToken()) {
            LOG.debug("Skip protection because protect_commands_with_access_token: false in configuration file.");
            return;
        }

        if (Util.isNullOrEmpty(authorization)) {
            LOG.debug("No access token provided in Authorization header. Forbidden.");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        String accessToken = authorization.substring(prefix.length());
        if (Util.isNullOrEmpty(accessToken)) {
            LOG.debug("No access token provided in Authorization header. Forbidden.");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        validationService.validateAccessToken(accessToken, authorizationRpId);
    }

    public MainPersistenceService getJansConfigurationService() {
        return jansConfigurationService;
    }

    public ValidationService getValidationService() {
        return validationService;
    }

    public RpSyncService getRpSyncService() {
        return rpSyncService;
    }
}
