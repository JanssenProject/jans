/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.net;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.URLPatternList;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shared validation and fetching for client-supplied sector_identifier_uri values, used both at
 * client registration time ({@code RegisterParamsValidator}) and at authorization time
 * ({@code RedirectionUriService}). Enforces https-only scheme and the {@code requestUriBlockList}
 * before any outbound request is made, to prevent SSRF via sector_identifier_uri.
 *
 * @author Yuriy Z
 */
@Stateless
@Named
public class SectorIdentifierUriService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    public boolean isAllowedSectorIdentifierUri(String sectorIdentifierUri) {
        URI uri;
        try {
            uri = new URI(sectorIdentifierUri);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                log.warn("sector_identifier_uri must use https scheme, got: {}", sectorIdentifierUri);
                return false;
            }
        } catch (URISyntaxException e) {
            log.warn("sector_identifier_uri is not a valid URI: {}", sectorIdentifierUri);
            return false;
        }

        if (isPrivateOrUnresolvableHost(uri.getHost())) {
            return false;
        }

        final List<String> blockList = appConfiguration.getRequestUriBlockList();
        if (blockList != null && !blockList.isEmpty()) {
            URLPatternList urlPatternList = new URLPatternList(blockList);
            if (urlPatternList.isUrlListed(sectorIdentifierUri)) {
                log.warn("sector_identifier_uri is forbidden by block list: {}", sectorIdentifierUri);
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves all IP addresses for the host and reports true if any resolves to a private/loopback/link-local
     * address, or if the host cannot be resolved at all (fail closed). Checking all records (not just the first)
     * mitigates DNS round-robin evasion.
     */
    boolean isPrivateOrUnresolvableHost(String host) {
        if (StringUtils.isBlank(host)) {
            log.warn("Rejecting sector_identifier_uri: host is blank (SectorIdentifierUriService#isPrivateOrUnresolvableHost)");
            return true;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                final String reason = PrivateAddressUtil.reasonForPrivateAddress(address);
                if (reason != null) {
                    log.warn("Rejecting sector_identifier_uri: host '{}' resolves to address '{}', which is a {} " +
                                    "(SectorIdentifierUriService#isPrivateOrUnresolvableHost, rule: PrivateAddressUtil#isPrivateAddress)",
                            host, address.getHostAddress(), reason);
                    return true;
                }
            }
            return false;
        } catch (UnknownHostException e) {
            log.warn("Rejecting sector_identifier_uri: host '{}' could not be resolved via DNS, failing closed " +
                    "(SectorIdentifierUriService#isPrivateOrUnresolvableHost): {}", host, e.getMessage());
            return true;
        }
    }

    public static boolean isPrivateAddress(InetAddress address) {
        return PrivateAddressUtil.isPrivateAddress(address);
    }

    public String fetchSectorIdentifierContent(String sectorIdentifierUri) {
        try (jakarta.ws.rs.client.Client clientRequest = ClientBuilder.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
             Response clientResponse = clientRequest.target(sectorIdentifierUri).request().buildGet().invoke()) {
            if (clientResponse.getStatus() != 200) {
                return null;
            }
            return clientResponse.readEntity(String.class);
        }
    }
}
