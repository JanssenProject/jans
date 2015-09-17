/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.OpenIdConfigurationClient;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.UmaConfiguration;

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

    private final ConcurrentMap<String, OpenIdConfigurationResponse> m_map = new ConcurrentHashMap<String, OpenIdConfigurationResponse>();
    private final ConcurrentMap<String, UmaConfiguration> m_umaMap = new ConcurrentHashMap<String, UmaConfiguration>();

    /**
     * Singleton
     */
    private static final DiscoveryService INSTANCE = new DiscoveryService(null);

    private final Configuration configuration;

    public DiscoveryService(Configuration configuration) {
        this.configuration = configuration;
    }

    public static DiscoveryService getInstance() {
        return INSTANCE;
    }

    public OpenIdConfigurationResponse getDiscoveryResponse(String p_discoveryUrl) {
        try {
            if (StringUtils.isNotBlank(p_discoveryUrl)) {
                final OpenIdConfigurationResponse r = m_map.get(p_discoveryUrl);
                if (r != null) {
                    return r;
                }
                final OpenIdConfigurationClient client = new OpenIdConfigurationClient(p_discoveryUrl);
                client.setExecutor(HttpService.getInstance().getClientExecutor());
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

    public UmaConfiguration getUmaDiscovery(String p_umaDiscoveryUrl) {
        try {
            if (StringUtils.isNotBlank(p_umaDiscoveryUrl)) {
                final UmaConfiguration r = m_umaMap.get(p_umaDiscoveryUrl);
                if (r != null) {
                    return r;
                }
                final UmaConfiguration response = UmaClientFactory.instance().createMetaDataConfigurationService(
                        p_umaDiscoveryUrl, HttpService.getInstance().getClientExecutor()).getMetadataConfiguration();
                LOG.trace("Uma discovery response: {} ", response);
                m_umaMap.put(p_umaDiscoveryUrl, response);
                return response;
            } else {
                LOG.error("Uma discovery URL is null or blank.");
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch UMA discovery information for url: {}", p_umaDiscoveryUrl);
        return null;
    }
}
