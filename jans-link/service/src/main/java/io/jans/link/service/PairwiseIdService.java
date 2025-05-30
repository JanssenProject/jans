package io.jans.link.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

//import javax.inject.Inject;

import io.jans.link.model.GluuCustomPerson;
import io.jans.link.model.GluuUserPairwiseIdentifier;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PairwiseIdService implements IPairwiseIdService, Serializable {

	private static final long serialVersionUID = -758342433526960035L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private OrganizationService organizationService;

	@Override
	public boolean removePairWiseIdentifier(GluuCustomPerson person, GluuUserPairwiseIdentifier pairwiseIdentifier) {
		try {
			String finalDn = String.format("oxId=%s,ou=pairwiseIdentifiers,", pairwiseIdentifier.getOxId());
			finalDn = finalDn.concat(person.getDn());
			ldapEntryManager.removeRecursively(finalDn, GluuCustomPerson.class);
			return true;
		} catch (Exception e) {
			log.error("", e);
			return false;
		}
	}

	@Override
	public String getDnForPairWiseIdentifier(String oxid, String personInum) {
		String orgDn = organizationService.getDnForOrganization();
		if (!StringHelper.isEmpty(personInum) && StringHelper.isEmpty(oxid)) {
			return String.format("ou=pairwiseIdentifiers,inum=%s,ou=people,%s", personInum, orgDn);
		}
		if (!StringHelper.isEmpty(oxid) && !StringHelper.isEmpty(personInum)) {
			return String.format("oxId=%s,ou=pairwiseIdentifiers,inum=%s,ou=people,%s", oxid, personInum, orgDn);
		}
		return "";
	}

	@Override
	public List<GluuUserPairwiseIdentifier> findAllUserPairwiseIdentifiers(GluuCustomPerson person) {
		try {
			List<GluuUserPairwiseIdentifier> results = ldapEntryManager.findEntries(
					getDnForPairWiseIdentifier(null, person.getInum()), GluuUserPairwiseIdentifier.class, null);
			if (results == null) {
				return new ArrayList<GluuUserPairwiseIdentifier>();
			}
			return results;
		} catch (Exception e) {
			log.warn("Current user don't pairwise identifiers");
			return new ArrayList<GluuUserPairwiseIdentifier>();
		}

	}

}
