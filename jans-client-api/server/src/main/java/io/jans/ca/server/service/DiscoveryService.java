/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.service;

import com.google.inject.Inject;
import io.dropwizard.util.Strings;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.op.OpClientFactory;
import org.apache.commons.lang.StringUtils;
import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.ca.common.ErrorResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Yuriy Zabrovarnyy
 */

public class DiscoveryService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryService.class);

    public static final String WELL_KNOWN_CONNECT_PATH = "/.well-known/openid-configuration";

    public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma2-configuration";

    private final ConcurrentMap<String, OpenIdConfigurationResponse> map = new ConcurrentHashMap<String, OpenIdConfigurationResponse>();
    private final ConcurrentMap<String, UmaMetadata> umaMap = new ConcurrentHashMap<String, UmaMetadata>();

    private final HttpService httpService;
    private final RpSyncService rpSyncService;
    private final ValidationService validationService;
    private final OpClientFactory opClientFactory;

    @Inject
    public DiscoveryService(HttpService httpService, RpSyncService rpSyncService, ValidationService validationService, OpClientFactory opClientFactory) {
        this.httpService = httpService;
        this.rpSyncService = rpSyncService;
        this.validationService = validationService;
        this.opClientFactory = opClientFactory;
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponseByRpId(String rpId) {
        validationService.notBlankRpId(rpId);

        Rp rp = rpSyncService.getRp(rpId);
        return getConnectDiscoveryResponse(rp);
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponse(Rp rp) {
        return getConnectDiscoveryResponse(rp.getOpConfigurationEndpoint(), rp.getOpHost(), rp.getOpDiscoveryPath());
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponse(String opConfigurationEndpoint, String opHost, String opDiscoveryPath) {
        return Strings.isNullOrEmpty(opConfigurationEndpoint) ? getConnectDiscoveryResponse(getConnectDiscoveryUrl(opHost, opDiscoveryPath))
                : getConnectDiscoveryResponse(opConfigurationEndpoint);
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponse(String opConfigurationEndpoint) {
        validationService.validateOpConfigurationEndpoint(opConfigurationEndpoint);
        try {
            final OpenIdConfigurationResponse r = map.get(opConfigurationEndpoint);
            if (r != null) {
                validationService.isOpHostAllowed(r.getIssuer());
                return r;
            }
            final OpenIdConfigurationClient client = opClientFactory.createOpenIdConfigurationClient(opConfigurationEndpoint);
            client.setExecutor(httpService.getClientEngine());
            final OpenIdConfigurationResponse response = client.execOpenIdConfiguration();
            LOG.trace("Discovery response: {} ", response.getEntity());
            if (StringUtils.isNotBlank(response.getEntity())) {
                map.put(opConfigurationEndpoint, response);
                validationService.isOpHostAllowed(response.getIssuer());
                return response;
            } else {
                LOG.error("No response from discovery!");
            }
        } catch (SSLHandshakeException e) {
            LOG.error(e.getMessage(), e);
            throw new HttpException(ErrorResponseCode.SSL_HANDSHAKE_ERROR);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Internal server error. Message: " + e.getMessage()).build());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch discovery information for op_configuration_endpoint: {}", opConfigurationEndpoint);
        throw new HttpException(ErrorResponseCode.NO_CONNECT_DISCOVERY_RESPONSE);
    }

    public UmaMetadata getUmaDiscoveryByRpId(String rpId) {
        validationService.notBlankRpId(rpId);

        Rp rp = rpSyncService.getRp(rpId);
        return getUmaDiscovery(rp.getOpConfigurationEndpoint(), rp.getOpHost(), rp.getOpDiscoveryPath());
    }

    public UmaMetadata getUmaDiscovery(String opConfigurationEndpoint, String opHost, String opDiscoveryPath) {
        return Strings.isNullOrEmpty(opConfigurationEndpoint) ? getUmaDiscovery(getConnectDiscoveryUrl(opHost, opDiscoveryPath))
                : getUmaDiscovery(opConfigurationEndpoint);
    }

    public UmaMetadata getUmaDiscovery(String opConfigurationEndpoint) {
        validationService.validateOpConfigurationEndpoint(opConfigurationEndpoint);

        try {
            final UmaMetadata r = umaMap.get(opConfigurationEndpoint);
            if (r != null) {
                validationService.isOpHostAllowed(r.getIssuer());
                return r;
            }
            final UmaMetadata response = opClientFactory.createUmaClientFactory().createMetadataService(
                    getUmaDiscoveryUrl(opConfigurationEndpoint), httpService.getClientEngine()).getMetadata();
            LOG.trace("Uma discovery response: {} ", response);
            umaMap.put(opConfigurationEndpoint, response);
            validationService.isOpHostAllowed(response.getIssuer());
            return response;

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch UMA discovery information for op_configuration_endpoint: {}", opConfigurationEndpoint);
        throw new HttpException(ErrorResponseCode.NO_UMA_DISCOVERY_RESPONSE);
    }

    public String getConnectDiscoveryUrl(Rp rp) {
        return getConnectDiscoveryUrl(rp.getOpHost(), rp.getOpDiscoveryPath());
    }

    public String getConnectDiscoveryUrl(String opHost, String opDiscoveryPath) {
        String result = baseOpUrl(opHost);
        if (StringUtils.isNotBlank(opDiscoveryPath)) {
            result += opDiscoveryPath;
        }
        return result + WELL_KNOWN_CONNECT_PATH;
    }

    public String getUmaDiscoveryUrl(String opHost, String opDiscoveryPath) {
        String result = baseOpUrl(opHost);
        if (StringUtils.isNotBlank(opDiscoveryPath)) {
            result += opDiscoveryPath;
        }
        return result + WELL_KNOWN_UMA_PATH;
    }

    public String getUmaDiscoveryUrl(String opConfigurationEndpoint) {
        String result = baseOpUrl(opConfigurationEndpoint);
        result = result.replace(WELL_KNOWN_CONNECT_PATH, WELL_KNOWN_UMA_PATH);
        return result;
    }

    private String baseOpUrl(String opHost) {
        if (!opHost.startsWith("http")) {
            opHost = "https://" + opHost;
        }
        if (opHost.endsWith("/")) {
            opHost = StringUtils.removeEnd(opHost, "/");
        }
        return opHost;
    }

}
