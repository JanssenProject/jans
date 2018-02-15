package org.gluu.persist.model;

import java.util.List;

/**
 * Batch operation
 *
 * @author Yuriy Movchan Date: 01/29/2018
 */
public interface BatchOperation<T> {

    boolean collectSearchResult(int size);

    void performAction(List<T> entries);

}