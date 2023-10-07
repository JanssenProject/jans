package io.jans.keycloak.link.service;

import io.jans.keycloak.link.model.GluuCustomPerson;
import io.jans.keycloak.link.model.GluuUserPairwiseIdentifier;

import java.util.List;

public interface IPairwiseIdService {

	boolean removePairWiseIdentifier(GluuCustomPerson person, GluuUserPairwiseIdentifier pairwiseIdentifier);

	public abstract List<GluuUserPairwiseIdentifier> findAllUserPairwiseIdentifiers(GluuCustomPerson person);

	String getDnForPairWiseIdentifier(String oxid, String personInum);

}
