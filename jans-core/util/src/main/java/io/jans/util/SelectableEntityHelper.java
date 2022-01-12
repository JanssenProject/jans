/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util;

import io.jans.model.SelectableEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts list of entities to list of selectable entities
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

    public static <T> void select(List<SelectableEntity<T>> selectableEntities, List<T> entities) {
        for (SelectableEntity<T> selectable : selectableEntities) {
            selectable.setSelected(entities.contains(selectable.getEntity()));
        }
    }
}
