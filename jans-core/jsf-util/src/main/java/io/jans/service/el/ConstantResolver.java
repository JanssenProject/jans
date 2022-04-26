/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.el;

import java.beans.FeatureDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
public class ConstantResolver extends ELResolver {
    private final Map<String, Object> constants = new HashMap<String, Object>();

    public void addConstant(String name, Object value) {
        constants.put(name, value);
    }

    public void removeConstant(String name) {
        constants.remove(name);
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null && constants.containsKey(property)) {
            context.setPropertyResolved(true);
            return constants.get(property);
        }

        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        if (base == null && constants.containsKey(property)) {
            return constants.get(property).getClass();
        }

        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }
}
