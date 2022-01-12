/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import java.io.Serializable;

/**
 * Allows to select entity
 *
 * @author Yuriy Movchan Date: 04/25/2013
 */
public class SelectableEntity<T> implements Serializable {

    private static final long serialVersionUID = -894849388491054202L;

    private T entity;
    private boolean selected;

    public SelectableEntity(T entity, boolean selected) {
        this.entity = entity;
        this.selected = false;
    }

    public SelectableEntity(T entity) {
        this(entity, false);
    }

    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "SelectableEntity{" +
                "entity=" + entity +
                ", selected=" + selected +
                '}';
    }
}
