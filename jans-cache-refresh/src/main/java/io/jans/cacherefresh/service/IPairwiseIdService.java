package io.jans.cacherefresh.service;

import java.util.List;

import io.jans.cacherefresh.model.GluuCustomPerson;
import io.jans.cacherefresh.model.GluuUserPairwiseIdentifier;

public interface IPairwiseIdService {

	boolean removePairWiseIdentifier(GluuCustomPerson person, GluuUserPairwiseIdentifier pairwiseIdentifier);

	public abstract List<GluuUserPairwiseIdentifier> findAllUserPairwiseIdentifiers(GluuCustomPerson person);

	String getDnForPairWiseIdentifier(String oxid,String personInum);

}
