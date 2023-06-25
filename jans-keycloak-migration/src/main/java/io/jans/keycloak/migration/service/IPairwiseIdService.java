package io.jans.keycloak.migration.service;

import java.util.List;

import io.jans.keycloak.migration.model.GluuCustomPerson;
import io.jans.keycloak.migration.model.GluuUserPairwiseIdentifier;

public interface IPairwiseIdService {

	boolean removePairWiseIdentifier(GluuCustomPerson person, GluuUserPairwiseIdentifier pairwiseIdentifier);

	public abstract List<GluuUserPairwiseIdentifier> findAllUserPairwiseIdentifiers(GluuCustomPerson person);

	String getDnForPairWiseIdentifier(String oxid,String personInum);

}
