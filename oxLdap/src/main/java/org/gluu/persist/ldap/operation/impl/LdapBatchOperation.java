package org.gluu.persist.ldap.operation.impl;

import java.util.List;

import org.gluu.persist.model.BatchOperation;

/**
 * @author eugeniuparvan Date: 12/29/16
 * @author Yuriy Movchan Date: 02/07/2018
 */
public abstract class LdapBatchOperation<T> implements BatchOperation<T> {

	@Override
	public void iterateAllByChunks(int batchSize) {
	}

	public boolean collectSearchResult(int size) {
		return true;
	}

	public void processSearchResult(List<T> entries) {
	}

}
