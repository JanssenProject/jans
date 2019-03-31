/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.OpenIdConfigurationClient;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.server.HttpException;

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
    private final RpService rpService;
    private final ValidationService validationService;

    @Inject
    public DiscoveryService(HttpService httpService, RpService rpService, ValidationService validationService) {
        this.httpService = httpService;
        this.rpService = rpService;
        this.validationService = validationService;
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponseByOxdId(String oxdId) {
        validationService.notBlankOxdId(oxdId);

        Rp site = rpService.getRp(oxdId);
        return getConnectDiscoveryResponse(site);
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponse(Rp rp) {
        return getConnectDiscoveryResponse(rp.getOpHost(), rp.getOpDiscoveryPath());
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponse(String opHost, String opDiscoveryPath) {
        validationService.notBlankOpHost(opHost);

        try {
            final OpenIdConfigurationResponse r = map.get(opHost);
            if (r != null) {
                return r;
            }
            final OpenIdConfigurationClient client = new OpenIdConfigurationClient(getConnectDiscoveryUrl(opHost, opDiscoveryPath));
            client.setExecutor(httpService.getClientExecutor());
            final OpenIdConfigurationResponse response = client.execOpenIdConfiguration();
            LOG.trace("Discovery response: {} ", response.getEntity());
            if (StringUtils.isNotBlank(response.getEntity())) {
                map.put(opHost, response);
                return response;
            } else {
                LOG.error("No response from discovery!");
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch discovery information for op_host: {}", opHost);
        throw new HttpException(ErrorResponseCode.NO_CONNECT_DISCOVERY_RESPONSE);
    }

    public UmaMetadata getUmaDiscoveryByOxdId(String oxdId) {
        validationService.notBlankOxdId(oxdId);

        Rp site = rpService.getRp(oxdId);
        return getUmaDiscovery(site.getOpHost(), site.getOpDiscoveryPath());
    }

    public UmaMetadata getUmaDiscovery(String opHost, String opDiscoveryPath) {
        validationService.notBlankOpHost(opHost);

        try {
            final UmaMetadata r = umaMap.get(opHost);
            if (r != null) {
                return r;
            }
            final UmaMetadata response = UmaClientFactory.instance().createMetadataService(
                    getUmaDiscoveryUrl(opHost, opDiscoveryPath), httpService.getClientExecutor()).getMetadata();
            LOG.trace("Uma discovery response: {} ", response);
            umaMap.put(opHost, response);
            return response;

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch UMA discovery information for op_host: {}", opHost);
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
