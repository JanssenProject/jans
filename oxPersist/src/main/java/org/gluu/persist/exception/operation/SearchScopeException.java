/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception.operation;

/**
 * Exception thrown when a search scope problem occurs
 *
 * @author Yuriy Movchan Date: 29/01/2018
 */
public class SearchScopeException extends PersistenceException {

    private static final long serialVersionUID = -4554637442590218891L;

    public SearchScopeException(String message) {
        super(message);
    }

}
