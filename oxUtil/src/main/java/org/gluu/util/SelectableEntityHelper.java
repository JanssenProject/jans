/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

import java.util.ArrayList;
import java.util.List;

import org.gluu.model.SelectableEntity;

/**
 * Converts lis of entities to list of selectable entities
 *
 * @author Yuriy Movchan Date: 04/25/2013
 */
public final class SelectableEntityHelper {

    private SelectableEntityHelper() { }

    public static <T> List<SelectableEntity<T>> convertToSelectableEntityModel(List<T> entities) {
        List<SelectableEntity<T>> result = new ArrayList<SelectableEntity<T>>(entities.size());

        for (T entity : entities) {
            result.add(new SelectableEntity<T>(entity, false));
        }

        return result;
    }

    public static <T> List<T> convertToEntities(List<SelectableEntity<T>> selectableEntities) {
        List<T> result = new ArrayList<T>(selectableEntities.size());

        for (SelectableEntity<T> selectableEntity : selectableEntities) {
            result.add(selectableEntity.getEntity());
        }

        return result;
    }

}
