/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception.operation;

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
