package org.gluu.persist.model;

import java.util.List;

/**
 * Batch operation
 * 
 * @author Yuriy Movchan Date: 01/29/2018
 */
public interface BatchOperation<T> {

	List<T> getChunkOrNull(int batchSize);

	void performAction(List<T> objects);

	void iterateAllByChunks(int batchSize);

	void processSearchResult(List<T> entries);

}