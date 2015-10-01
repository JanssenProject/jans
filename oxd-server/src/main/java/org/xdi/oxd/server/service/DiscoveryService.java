/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.service;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.OpenIdConfigurationClient;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxd.server.Configuration;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class DiscoveryService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryService.class);

    public static final String WELL_KNOWN_CONNECT_PATH = "/.well-known/openid-configuration";

    public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma-configuration";

    private final ConcurrentMap<String, OpenIdConfigurationResponse> m_map = new ConcurrentHashMap<String, OpenIdConfigurationResponse>();
    private final ConcurrentMap<String, UmaConfiguration> m_umaMap = new ConcurrentHashMap<String, UmaConfiguration>();

    private final HttpService httpService;
    private final Configuration configuration;

    @Inject
    public DiscoveryService(HttpService httpService, Configuration configuration) {
        this.httpService = httpService;
        this.configuration = configuration;
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponse() {
        return getConnectDiscoveryResponse(getConnectDiscoveryUrl());
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponse(String p_discoveryUrl) {
        try {
            if (StringUtils.isNotBlank(p_discoveryUrl)) {
                final OpenIdConfigurationResponse r = m_map.get(p_discoveryUrl);
                if (r != null) {
                    return r;
                }
                final OpenIdConfigurationClient client = new OpenIdConfigurationClient(p_discoveryUrl);
                client.setExecutor(httpService.getClientExecutor());
                final OpenIdConfigurationResponse response = client.execOpenIdConfiguration();
                LOG.trace("Discovery response: {} ", response.getEntity());
                if (StringUtils.isNotBlank(response.getEntity())) {
                    m_map.put(p_discoveryUrl, response);
                } else {
                    LOG.error("No response from discovery!");
                }
                return response;
            } else {
                LOG.error("Discovery URL is null or blank.");
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch discovery information for url: {}", p_discoveryUrl);
        return null;
    }

    public UmaConfiguration getUmaDiscovery() {
        return getUmaDiscovery(getUmaDiscoveryUrl());
    }

    public UmaConfiguration getUmaDiscovery(String umaDiscoveryUrl) {
        try {
            if (StringUtils.isNotBlank(umaDiscoveryUrl)) {
                final UmaConfiguration r = m_umaMap.get(umaDiscoveryUrl);
                if (r != null) {
                    return r;
                }
                final UmaConfiguration response = UmaClientFactory.instance().createMetaDataConfigurationService(
                        umaDiscoveryUrl, httpService.getClientExecutor()).getMetadataConfiguration();
                LOG.trace("Uma discovery response: {} ", response);
                m_umaMap.put(umaDiscoveryUrl, response);
                return response;
            } else {
                LOG.error("Uma discovery URL is null or blank.");
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch UMA discovery information for url: {}", umaDiscoveryUrl);
        return null;
    }

    public String getConnectDiscoveryUrl() {
        return baseOpUrl() + WELL_KNOWN_CONNECT_PATH;
    }

    public String getUmaDiscoveryUrl() {
        return baseOpUrl() + WELL_KNOWN_UMA_PATH;
    }

    private String baseOpUrl() {
        String baseUrl = configuration.getOpHost();

        if (!baseUrl.startsWith("http")) {
            baseUrl = "https://" + baseUrl;
        }
        if (!baseUrl.endsWith(File.separator)) {
            baseUrl = baseUrl + File.separator;
        }
        return baseUrl;
    }

}
