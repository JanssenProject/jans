/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model;

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

}
