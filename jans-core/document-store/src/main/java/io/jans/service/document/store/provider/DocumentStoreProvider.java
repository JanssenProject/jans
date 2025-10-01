/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.provider;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
public abstract class DocumentStoreProvider<T> implements DocumentStore<T> {

    public abstract void create();

    public abstract void destroy();

}
