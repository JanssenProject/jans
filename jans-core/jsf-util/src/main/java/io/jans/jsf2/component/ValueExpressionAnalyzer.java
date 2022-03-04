/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.component;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Locale;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;
import jakarta.el.VariableMapper;
import jakarta.faces.el.CompositeComponentExpressionHolder;

class ValueExpressionAnalyzer {
    private ValueExpression expression;

    ValueExpressionAnalyzer(ValueExpression expression) {
        this.expression = expression;
    }

    public ValueReference getValueReference(ELContext elContext) {
        InterceptingResolver resolver = new InterceptingResolver(elContext.getELResolver());
        try {
            expression.setValue(decorateELContext(elContext, resolver), null);
        } catch (ELException ele) {
            return null;
        }
        ValueReference reference = resolver.getValueReference();
        if (reference != null) {
            Object base = reference.getBase();
            if (base instanceof CompositeComponentExpressionHolder) {
                ValueExpression ve = ((CompositeComponentExpressionHolder) base)
                        .getExpression((String) reference.getProperty());
                if (ve != null) {
                    this.expression = ve;
                    reference = getValueReference(elContext);
                }
            }
        }
        return reference;
    }

    private ELContext decorateELContext(final ELContext context, final ELResolver resolver) {
        return new ELContext() {
            // punch in our new ELResolver
            @Override
            public ELResolver getELResolver() {
                return resolver;
            }

            // The rest of the methods simply delegate to the existing context
            @Override
            public Object getContext(Class key) {
                return context.getContext(key);
            }

            @Override
            public Locale getLocale() {
                return context.getLocale();
            }

            @Override
            public boolean isPropertyResolved() {
                return context.isPropertyResolved();
            }

            @Override
            public void putContext(Class key, Object contextObject) {
                context.putContext(key, contextObject);
            }

            @Override
            public void setLocale(Locale locale) {
                context.setLocale(locale);
            }

            @Override
            public void setPropertyResolved(boolean resolved) {
                context.setPropertyResolved(resolved);
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return context.getFunctionMapper();
            }

            @Override
            public VariableMapper getVariableMapper() {
                return context.getVariableMapper();
            }
        };
    }

    private static class InterceptingResolver extends ELResolver {
        private ELResolver delegate;
        private ValueReference valueReference;

        InterceptingResolver(ELResolver delegate) {
            this.delegate = delegate;
        }

        public ValueReference getValueReference() {
            return valueReference;
        }

        // Capture the base and property rather than write the value
        @Override
        public void setValue(ELContext context, Object base, Object property, Object value) {
            if (base != null && property != null) {
                context.setPropertyResolved(true);
                valueReference = new ValueReference(base, property.toString());
            }
        }

        // The rest of the methods simply delegate to the existing context
        @Override
        public Object getValue(ELContext context, Object base, Object property) {
            return delegate.getValue(context, base, property);
        }

        @Override
        public Class<?> getType(ELContext context, Object base, Object property) {
            return delegate.getType(context, base, property);
        }

        @Override
        public boolean isReadOnly(ELContext context, Object base, Object property) {
            return delegate.isReadOnly(context, base, property);
        }

        @Override
        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
            return delegate.getFeatureDescriptors(context, base);
        }

        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            return delegate.getCommonPropertyType(context, base);
        }
    }
}
