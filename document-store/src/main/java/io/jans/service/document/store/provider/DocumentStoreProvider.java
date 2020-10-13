/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
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
