/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

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
