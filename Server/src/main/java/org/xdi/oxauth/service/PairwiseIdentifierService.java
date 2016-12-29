package org.xdi.oxauth.service;

import com.unboundid.ldap.sdk.Filter;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.common.PairwiseIdType;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.ldap.PairwiseIdentifier;
import org.xdi.oxauth.model.util.SubjectIdentifierGenerator;
import org.xdi.util.StringHelper;

import java.net.URI;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
@Scope(ScopeType.STATELESS)
@Name("pairwiseIdentifierService")
@AutoCreate
public class PairwiseIdentifierService {

    @In
    private LdapEntryManager ldapEntryManager;

    @In
    private UserService userService;

    @Logger
    private Log log;
    @In
    private AppConfiguration appConfiguration;

    public void addBranch(final String userInum) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("pairwiseIdentifiers");
        branch.setDn(getBaseDnForPairwiseIdentifiers(userInum));

        ldapEntryManager.persist(branch);
    }

    public boolean containsBranch(final String userInum) {
        return ldapEntryManager.contains(SimpleBranch.class, getBaseDnForPairwiseIdentifiers(userInum));
    }

    public void prepareBranch(final String userInum) {
        // Create pairwise identifier branch if needed
        if (!containsBranch(userInum)) {
            addBranch(userInum);
        }
    }

    public PairwiseIdentifier findPairWiseIdentifier(String userInum, String sectorIdentifierUri) throws Exception {
        PairwiseIdType pairwiseIdType = PairwiseIdType.fromString(appConfiguration.getPairwiseIdType());
        String sectorIdentifier = URI.create(sectorIdentifierUri).getHost();

        if (PairwiseIdType.PERSISTENT == pairwiseIdType) {
            prepareBranch(userInum);

            String baseDnForPairwiseIdentifiers = getBaseDnForPairwiseIdentifiers(userInum);
            Filter filter = Filter.createEqualityFilter("oxSectorIdentifier", sectorIdentifier);

            List<PairwiseIdentifier> entries = ldapEntryManager.findEntries(baseDnForPairwiseIdentifiers, PairwiseIdentifier.class, filter);
            if (entries != null && !entries.isEmpty()) {
                // if more then one entry then it's problem, non-deterministic behavior, id must be unique
                if (entries.size() > 1) {
                    log.error("Found more then one pairwise identifier by sector identifier: {0}" + sectorIdentifier);
                    for (PairwiseIdentifier pairwiseIdentifier : entries) {
                        log.error(pairwiseIdentifier);
                    }
                }
                return entries.get(0);
            }
        } else { // PairwiseIdType.ALGORITHMIC
            String key = appConfiguration.getPairwiseCalculationKey();
            String salt = appConfiguration.getPairwiseCalculationSalt();

            String calculatedSub = SubjectIdentifierGenerator.generatePairwiseSubjectIdentifier(
                    sectorIdentifierUri, userInum, key, salt, appConfiguration);

            PairwiseIdentifier pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifierUri);
            pairwiseIdentifier.setId(calculatedSub);

            return pairwiseIdentifier;
        }

        return null;
    }

    public void addPairwiseIdentifier(String userInum, PairwiseIdentifier pairwiseIdentifier) {
        prepareBranch(userInum);
        userService.addUserAttributeByUserInum(userInum, "oxPPID", pairwiseIdentifier.getId());

        ldapEntryManager.persist(pairwiseIdentifier);
    }

    public String getDnForPairwiseIdentifier(String oxId, String userInum) {
        String baseDn = getBaseDnForPairwiseIdentifiers(userInum);
        if (StringHelper.isEmpty(oxId)) {
            return baseDn;
        }
        return String.format("oxId=%s,%s", oxId, baseDn);
    }

    public String getBaseDnForPairwiseIdentifiers(String userInum) {
        final String userBaseDn = userService.getDnForUser(userInum); // "ou=pairwiseIdentifiers,inum=1234,ou=people,o=@!1111,o=gluu"
        return String.format("ou=pairwiseIdentifiers,%s", userBaseDn);
    }

    public static PairwiseIdentifierService instance() {
        return (PairwiseIdentifierService) Component.getInstance(PairwiseIdentifierService.class);
    }
}
