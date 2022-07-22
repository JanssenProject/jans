/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.operation;

import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.exception.operation.EntryConvertationException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.extension.PersistenceExtension;

/**
 * Base interface for Operation Service
 *
 * @author Yuriy Movchan Date: 06/22/2018
 */
public interface PersistenceOperationService {

	boolean isConnected();

    boolean authenticate(String key, String password, String objectClass) throws ConnectionException, SearchException, AuthenticationException, EntryConvertationException;

	public void setPersistenceExtension(PersistenceExtension persistenceExtension);

	public boolean isSupportObjectClass(String objectClass);

}
