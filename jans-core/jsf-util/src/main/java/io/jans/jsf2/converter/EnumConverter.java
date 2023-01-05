/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.converter;

import java.util.Collection;

import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

@FacesConverter(value = "io.jans.jsf2.converter.EnumConverter")
public class EnumConverter implements Converter {

    public Object getAsObject(FacesContext context, UIComponent comp, String value) throws ConverterException {
        try {
            return getAsObjectImpl(context, comp, value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Object getAsObjectImpl(FacesContext context, UIComponent comp, String value) throws ConverterException {
        ValueExpression expr = comp.getValueExpression("value");

        Class enumType = expr == null ? null : expr.getType(context.getELContext());
        if (enumType != null && enumType.isEnum()) {
            return Enum.valueOf(enumType, value);
        } else {
            for (Object child : comp.getChildren()) {
                if (child instanceof UIComponent) {
                    UIComponent c = (UIComponent) child;
                    expr = c.getValueExpression("value");
                    Object val = expr == null ? null : expr.getValue(context.getELContext());
                    if (val == null) {
                        throw new ConverterException("Cannot get items");
                    }

                    Class t = val.getClass();
                    if (t.isArray() && t.getComponentType().isEnum()) {
                        return Enum.valueOf(t.getComponentType(), value);
                    } else if (val instanceof Collection) {
                        Object firstItem = ((Collection) val).iterator().next();
                        if (firstItem instanceof Enum) {
                            t = ((Enum) firstItem).getDeclaringClass();
                        } else {
                            t = firstItem.getClass();
                        }

                        return Enum.valueOf(t, value);
                    }
                }
            }
        }

        throw new ConverterException("Unable to find selectItems with enum values.");
    }

    public String getAsString(FacesContext context, UIComponent component, Object object) throws ConverterException {
        return object == null ? null : ((Enum) object).name();
    }

}
