/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.json;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JsonApplier {

    private static final Logger log = Logger.getLogger(JsonApplier.class);

    private static final JsonApplier APPLIER = new JsonApplier();

    private JsonApplier() {
    }

    public static JsonApplier getInstance() {
        return APPLIER;
    }

    public void apply(Object source, JSONObject target) {
        for (PropertyDefinition definition : PropertyDefinition.values()) {
            apply(source, target, definition);
        }
    }

    public void apply(Object source, Map<String, String> parameters) {
        for (PropertyDefinition definition : PropertyDefinition.values()) {
            apply(source, parameters, definition);
        }
    }

    private void apply(Object source, Map<String, String> target, PropertyDefinition property) {
        try {
            if (!isAllowed(source, target, property, source.getClass())) {
                return;
            }

            Field field = source.getClass().getDeclaredField(property.getJavaTargetPropertyName());
            field.setAccessible(true);
            Object value = field.get(source);
            if (value == null) {
                return;
            }

            if (String.class.isAssignableFrom(property.getJavaType())) {

                target.put(property.getJsonName(), (String) value);
                return;
            }
            if (Collection.class.isAssignableFrom(property.getJavaType())) {
                Collection valueAsCollection = (Collection) field.get(source);
                target.put(property.getJsonName(), new JSONArray(valueAsCollection).toString());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void apply(Object source, JSONObject target, PropertyDefinition property) {
        try {
            if (!isAllowed(source, target, property, source.getClass())) {
                return;
            }

            Field field = source.getClass().getDeclaredField(property.getJavaTargetPropertyName());
            field.setAccessible(true);
            Object value = field.get(source);

            if (String.class.isAssignableFrom(property.getJavaType())) {
                target.put(property.getJsonName(), value);
                return;
            }
            if (Collection.class.isAssignableFrom(property.getJavaType())) {
                Collection valueAsCollection = (Collection) field.get(source);
                target.put(property.getJsonName(), valueAsCollection);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean isAllowed(Object source, Object target, PropertyDefinition property, Class<?>... clazzesToCheck) {
        if (source == null || target == null || property == null || clazzesToCheck == null || clazzesToCheck.length == 0) {
            return false;
        }

        try {
            final Set<String> allowedClasses = property.getJavaTargetsClassNamesAsStrings();

            for (Class<?> clazzToCheck : clazzesToCheck) {
                if (!allowedClasses.contains(clazzToCheck.getName())) {
                    return false;
                }

                Field field = clazzToCheck.getDeclaredField(property.getJavaTargetPropertyName());

                final Class javaType = property.getJavaType();
                if (!field.getType().isAssignableFrom(javaType)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void apply(JSONObject source, Object target) {
        for (PropertyDefinition definition : PropertyDefinition.values()) {
            apply(source, target, definition);
        }
    }

    public void apply(JSONObject source, Object target, PropertyDefinition property) {
        try {
            if (!source.has(property.getJsonName())) {
                return;
            }
            if (!isAllowed(source, target, property, target.getClass())) {
                return;
            }

            Field field = target.getClass().getDeclaredField(property.getJavaTargetPropertyName());
            field.setAccessible(true);


            Object valueToSet = null;

            if (String.class.isAssignableFrom(property.getJavaType())) {
                valueToSet = source.optString(property.getJsonName());
            }
            if (Collection.class.isAssignableFrom(property.getJavaType())) {
                final JSONArray jsonArray = source.getJSONArray(property.getJsonName());
                valueToSet = jsonArray.toList();
            }

            field.set(target, valueToSet);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Transfer between two java objects
     */
    public void transfer(Object source, Object target) {
        for (PropertyDefinition definition : PropertyDefinition.values()) {
            transfer(source, target, definition);
        }
    }

    private void transfer(Object source, Object target, PropertyDefinition property) {
        try {
            if (!isAllowed(source, target, property, source.getClass(), target.getClass())) {
                return;
            }

            Field sourceField = source.getClass().getDeclaredField(property.getJavaTargetPropertyName());
            sourceField.setAccessible(true);

            Field targetField = target.getClass().getDeclaredField(property.getJavaTargetPropertyName());
            targetField.setAccessible(true);


            Object valueToSet = null;

            if (String.class.isAssignableFrom(property.getJavaType()) || Collection.class.isAssignableFrom(property.getJavaType())) {
                valueToSet = sourceField.get(source);
            }

            if (valueToSet != null) {
                targetField.set(target, valueToSet);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static List<String> getStringList(JSONArray jsonArray) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            String value = jsonArray.optString(i);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }
}
