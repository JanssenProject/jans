package io.jans.link.service;

import java.util.List;

import io.jans.link.model.GluuCustomPerson;
import io.jans.link.model.GluuUserPairwiseIdentifier;

public interface IPairwiseIdService {

	boolean removePairWiseIdentifier(GluuCustomPerson person, GluuUserPairwiseIdentifier pairwiseIdentifier);

	public abstract List<GluuUserPairwiseIdentifier> findAllUserPairwiseIdentifiers(GluuCustomPerson person);

	String getDnForPairWiseIdentifier(String oxid,String personInum);

}
