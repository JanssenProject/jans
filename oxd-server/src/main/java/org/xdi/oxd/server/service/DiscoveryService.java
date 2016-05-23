/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import com.google.inject.Inject;
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

    public static final String WELL_KNOWN_CONNECT_PATH = "/.well-known/openid-configuration";

    public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma-configuration";

    private final ConcurrentMap<String, OpenIdConfigurationResponse> m_map = new ConcurrentHashMap<String, OpenIdConfigurationResponse>();
    private final ConcurrentMap<String, UmaConfiguration> m_umaMap = new ConcurrentHashMap<String, UmaConfiguration>();

    private final HttpService httpService;
    private final SiteConfigurationService siteService;

    @Inject
    public DiscoveryService(HttpService httpService, SiteConfigurationService siteService) {
        this.httpService = httpService;
        this.siteService = siteService;
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponseByOxdId(String oxdId) {
        SiteConfiguration site = siteService.getSite(oxdId);
        if (site != null) {
            return getConnectDiscoveryResponse(site.getOpHost());
        }
        LOG.error("Failed to find site configuration for oxd_id: " + oxdId);
        return null;
    }

    public OpenIdConfigurationResponse getConnectDiscoveryResponse(String opHost) {
        if (Strings.isNullOrEmpty(opHost)) {
            LOG.error("Failed to fetch connect discovery response. op_host is null or blank.");
            return null;
        }

        try {
            final OpenIdConfigurationResponse r = m_map.get(opHost);
            if (r != null) {
                return r;
            }
            final OpenIdConfigurationClient client = new OpenIdConfigurationClient(getConnectDiscoveryUrl(opHost));
            client.setExecutor(httpService.getClientExecutor());
            final OpenIdConfigurationResponse response = client.execOpenIdConfiguration();
            LOG.trace("Discovery response: {} ", response.getEntity());
            if (StringUtils.isNotBlank(response.getEntity())) {
                m_map.put(opHost, response);
            } else {
                LOG.error("No response from discovery!");
            }
            return response;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch discovery information for op_host: {}", opHost);
        return null;
    }

    public UmaConfiguration getUmaDiscovery(String opHost) {

        if (Strings.isNullOrEmpty(opHost)) {
            LOG.error("Failed to fetch UMA discovery response. op_host is null or blank.");
            return null;
        }

        try {
            final UmaConfiguration r = m_umaMap.get(opHost);
            if (r != null) {
                return r;
            }
            final UmaConfiguration response = UmaClientFactory.instance().createMetaDataConfigurationService(
                    getUmaDiscoveryUrl(opHost), httpService.getClientExecutor()).getMetadataConfiguration();
            LOG.trace("Uma discovery response: {} ", response);
            m_umaMap.put(opHost, response);
            return response;

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to fetch UMA discovery information for op_host: {}", opHost);
        return null;
    }

    public String getConnectDiscoveryUrl(String opHost) {
        return baseOpUrl(opHost) + WELL_KNOWN_CONNECT_PATH;
    }

    public String getUmaDiscoveryUrl(String opHost) {
        return baseOpUrl(opHost) + WELL_KNOWN_UMA_PATH;
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
