package org.gluu.persist.model;

/**
 * @author Yuriy Movchan Date: 02/07/2018
 */
public abstract class DefaultBatchOperation<T> implements BatchOperation<T> {

	public boolean collectSearchResult(int size) {
		return true;
	}

}
