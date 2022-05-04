/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.service.common.UserService;
import io.jans.as.model.common.PairwiseIdType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.SubjectIdentifierGenerator;
import io.jans.as.persistence.model.PairwiseIdentifier;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version May 7, 2019
 */
@Stateless
@Named
public class PairwiseIdentifierService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private UserService userService;

    @Inject
    private AppConfiguration appConfiguration;

    public void addBranch(final String userInum) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("pairwiseIdentifiers");
        branch.setDn(getBaseDnForPairwiseIdentifiers(userInum));

        ldapEntryManager.persist(branch);
    }

    public boolean containsBranch(final String userInum) {
        return ldapEntryManager.contains(getBaseDnForPairwiseIdentifiers(userInum), SimpleBranch.class);
    }

    public void prepareBranch(final String userInum) {
        if (!ldapEntryManager.hasBranchesSupport(userService.getDnForUser(userInum))) {
            return;
        }

        // Create pairwise identifier branch if needed
        if (!containsBranch(userInum)) {
            addBranch(userInum);
        }
    }

    public PairwiseIdentifier findPairWiseIdentifier(String userInum, String sectorIdentifier, String clientId) throws Exception {
        PairwiseIdType pairwiseIdType = PairwiseIdType.fromString(appConfiguration.getPairwiseIdType());

        if (PairwiseIdType.PERSISTENT == pairwiseIdType) {
            prepareBranch(userInum);

            String baseDnForPairwiseIdentifiers = getBaseDnForPairwiseIdentifiers(userInum);

            final Filter filter;
            if (appConfiguration.isShareSubjectIdBetweenClientsWithSameSectorId()) {
                Filter sectorIdentifierFilter = Filter.createEqualityFilter("jansSectorIdentifier", sectorIdentifier);
                Filter userInumFilter = Filter.createEqualityFilter("jansUsrId", userInum);

                filter = Filter.createANDFilter(sectorIdentifierFilter, userInumFilter);
            } else {
                Filter sectorIdentifierFilter = Filter.createEqualityFilter("jansSectorIdentifier", sectorIdentifier);
                Filter clientIdFilter = Filter.createEqualityFilter("jansClntId", clientId);
                Filter userInumFilter = Filter.createEqualityFilter("jansUsrId", userInum);

                filter = Filter.createANDFilter(sectorIdentifierFilter, clientIdFilter, userInumFilter);
            }

            List<PairwiseIdentifier> entries = ldapEntryManager.findEntries(baseDnForPairwiseIdentifiers, PairwiseIdentifier.class, filter);
            if (entries != null && !entries.isEmpty()) {
                // if more then one entry then it's problem, non-deterministic behavior, id must be unique
                if (entries.size() > 1) {
                    log.error("Found more then one pairwise identifier by sector identifier: {}" + sectorIdentifier);
                    for (PairwiseIdentifier pairwiseIdentifier : entries) {
                        log.error("PairwiseIdentifier: {}", pairwiseIdentifier);
                    }
                }
                return entries.get(0);
            }
        } else { // PairwiseIdType.ALGORITHMIC
            String key = appConfiguration.getPairwiseCalculationKey();
            String salt = appConfiguration.getPairwiseCalculationSalt();
            String localAccountId = appConfiguration.isShareSubjectIdBetweenClientsWithSameSectorId() ?
                    userInum : userInum + clientId;

            String calculatedSub = SubjectIdentifierGenerator.generatePairwiseSubjectIdentifier(
                    sectorIdentifier, localAccountId, key, salt, appConfiguration);

            PairwiseIdentifier pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifier, clientId, userInum);
            pairwiseIdentifier.setId(calculatedSub);

            return pairwiseIdentifier;
        }

        return null;
    }

    public void addPairwiseIdentifier(String userInum, PairwiseIdentifier pairwiseIdentifier) {
        prepareBranch(userInum);
        userService.addUserAttributeByUserInum(userInum, "jansPPID", pairwiseIdentifier.getId());

        ldapEntryManager.persist(pairwiseIdentifier);
    }

    public String getDnForPairwiseIdentifier(String jsId, String userInum) {
        String baseDn = getBaseDnForPairwiseIdentifiers(userInum);
        if (StringHelper.isEmpty(jsId)) {
            return baseDn;
        }
        return String.format("jansId=%s,%s", jsId, baseDn);
    }

    public String getBaseDnForPairwiseIdentifiers(String userInum) {
        final String userBaseDn = userService.getDnForUser(userInum); // "ou=pairwiseIdentifiers,inum=1234,ou=people,o=jans"
        return String.format("ou=pairwiseIdentifiers,%s", userBaseDn);
    }

}
