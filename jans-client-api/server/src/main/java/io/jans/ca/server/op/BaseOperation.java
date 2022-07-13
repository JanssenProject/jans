/*
  All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.util.Util;
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
import io.jans.ca.server.service.HttpService;
import io.jans.ca.server.service.RpSyncService;
import io.jans.ca.server.service.ValidationService;
import io.jans.ca.server.utils.Convertor;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@RequestScoped
@Named
public abstract class BaseOperation<T extends IParams> implements IOperation<T> {

    private static final Logger LOG = LoggerFactory.getLogger(BaseOperation.class);

    private static final String LOCALHOST_IP_ADDRESS = "127.0.0.1";

    @Inject
    ValidationService validationService;
    @Inject
    RpSyncService rpSyncService;
    @Inject
    HttpService httpService;
    @Inject
    MainPersistenceService jansConfigurationService;

    public Response process(String paramsAsString, HttpServletRequest httpRequest) {
        String endPointUrl = httpRequest.getRequestURL().toString();
        LOG.info("Endpoint: {}", endPointUrl);
        LOG.info("Request parameters: {}", paramsAsString);

        validateIpAddressAllowed(httpRequest.getRemoteAddr());

        Object forJsonConversion = getObjectForJsonConversion(paramsAsString, getParameterClass(), httpRequest);
        String response = null;

        if (getReturnType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
            response = Jackson2.asJsonSilently(forJsonConversion);
        } else if (getReturnType().equalsIgnoreCase(MediaType.TEXT_PLAIN)) {
            response = forJsonConversion.toString();
        }

        LOG.trace("Send back response: {}", response);
        return Response.ok(response).build();
    }

    private <T extends IParams> Object getObjectForJsonConversion(String paramsAsString, Class<T> paramsClass, HttpServletRequest httpRequest) {
        LOG.trace("Command: {}", paramsAsString);
        T params = read(safeToJson(paramsAsString), paramsClass);
        JsonNode jsonNodeParams = JsonNodeFactory.instance.pojoNode(params);

        final IOpResponse response = internProcess(jsonNodeParams, httpRequest);
        Object forJsonConversion = response;
        if (response instanceof POJOResponse) {
            forJsonConversion = ((POJOResponse) response).getNode();
        }
        return forJsonConversion;
    }

    private IOpResponse internProcess(JsonNode jsonNodeParams, HttpServletRequest httpRequest) {
        try {
            IParams iParams = Convertor.asParams(getParameterClass(), jsonNodeParams);
            validationService.validate(iParams);

            IOpResponse operationResponse = execute((T) iParams, httpRequest);
            if (operationResponse != null) {
                return operationResponse;
            } else {
                LOG.error("No response from operation. Endpoint: {}", httpRequest.getRequestURL().toString());
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

    public AuthCryptoProvider getCryptoProvider() throws Exception {
        ApiAppConfiguration conf = getJansConfigurationService().find();
        return new AuthCryptoProvider(conf.getCryptProviderKeyStorePath(), conf.getCryptProviderKeyStorePassword(), conf.getCryptProviderDnName());
    }

    public HttpService getHttpService() {
        return httpService;
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
