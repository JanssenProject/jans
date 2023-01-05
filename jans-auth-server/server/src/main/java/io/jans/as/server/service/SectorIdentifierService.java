/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.persistence.model.PairwiseIdentifier;
import io.jans.as.persistence.model.SectorIdentifier;
import io.jans.as.server.model.common.CIBAGrant;
import io.jans.as.server.model.common.IAuthorizationGrant;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URI;
import java.util.UUID;

/**
 * @author Javier Rojas Blum
 * @version February 11, 2022
 */
@Stateless
@Named
public class SectorIdentifierService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private PairwiseIdentifierService pairwiseIdentifierService;

    @Inject
    protected AppConfiguration appConfiguration;

    /**
     * Get sector identifier by jsId
     *
     * @param jsId Sector identifier jsId
     * @return Sector identifier
     */
    public SectorIdentifier getSectorIdentifierById(String jsId) {
        SectorIdentifier result = null;
        try {
            result = ldapEntryManager.find(SectorIdentifier.class, getDnForSectorIdentifier(jsId));
        } catch (Exception e) {
            log.error("Failed to find sector identifier by jsId " + jsId, e);
        }
        return result;
    }

    /**
     * Build DN string for sector identifier
     *
     * @param jsId Sector Identifier jsId
     * @return DN string for specified sector identifier or DN for sector identifiers branch if jsId is null
     * @throws Exception
     */
    public String getDnForSectorIdentifier(String jsId) {
        String sectorIdentifierDn = staticConfiguration.getBaseDn().getSectorIdentifiers();
        if (StringHelper.isEmpty(jsId)) {
            return sectorIdentifierDn;
        }

        return String.format("jansId=%s,%s", jsId, sectorIdentifierDn);
    }

    public String getSub(IAuthorizationGrant grant) {
        Client client = grant.getClient();
        User user = grant.getUser();

        if (user == null) {
            log.trace("User is null, return blank sub");
            return "";
        }
        if (client == null) {
            log.trace("Client is null, return blank sub.");
            return "";
        }

        return getSub(client, user, grant instanceof CIBAGrant);
    }

    public String getSub(Client client, User user, boolean isCibaGrant) {
        if (user == null) {
            log.trace("User is null, return blank sub");
            return "";
        }
        if (client == null) {
            log.trace("Client is null, return blank sub.");
            return "";
        }

        final boolean isClientPairwise = SubjectType.PAIRWISE.equals(client.getSubjectType());
        if (isClientPairwise) {
            final String sectorIdentifierUri;

            if (StringUtils.isNotBlank(client.getSectorIdentifierUri())) {
                sectorIdentifierUri = client.getSectorIdentifierUri();
            } else {
                if (!isCibaGrant) {
                    sectorIdentifierUri = !ArrayUtils.isEmpty(client.getRedirectUris()) ? client.getRedirectUris()[0] : null;
                } else {
                    if (client.getBackchannelTokenDeliveryMode() == io.jans.as.model.common.BackchannelTokenDeliveryMode.PUSH) {
                        sectorIdentifierUri = client.getBackchannelClientNotificationEndpoint();
                    } else {
                        sectorIdentifierUri = client.getJwksUri();
                    }
                }
            }

            String userInum = user.getAttribute("inum");

            try {
                if (StringUtils.isNotBlank(sectorIdentifierUri)) {
                    String sectorIdentifier = URI.create(sectorIdentifierUri).getHost();

                    PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(userInum,
                            sectorIdentifier, client.getClientId());
                    if (pairwiseIdentifier == null) {
                        pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifier, client.getClientId(), userInum);
                        pairwiseIdentifier.setId(UUID.randomUUID().toString());
                        pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(pairwiseIdentifier.getId(), userInum));
                        pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
                    }
                    return pairwiseIdentifier.getId();
                } else {
                    log.trace("Sector identifier uri is blank for client: " + client.getClientId());
                }
            } catch (Exception e) {
                log.error("Failed to get sub claim. PairwiseIdentifierService failed to find pair wise identifier.", e);
                return "";
            }
        }

        String openidSubAttribute = appConfiguration.getOpenidSubAttribute();
        if (Boolean.TRUE.equals(appConfiguration.getPublicSubjectIdentifierPerClientEnabled())
                && StringUtils.isNotBlank(client.getAttributes().getPublicSubjectIdentifierAttribute())) {
            openidSubAttribute = client.getAttributes().getPublicSubjectIdentifierAttribute();
        }
        if (StringHelper.equalsIgnoreCase(openidSubAttribute, "uid")) {
            return user.getUserId();
        }
        return user.getAttribute(openidSubAttribute);
    }
}
