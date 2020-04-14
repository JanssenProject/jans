package org.gluu.service.document.store.provider;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
public abstract class DocumentStoreProvider<T> implements DocumentStore<T> {

    public abstract void create();

    public abstract void destroy();

}
