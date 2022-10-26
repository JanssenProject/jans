/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.el;

import jakarta.el.ArrayELResolver;
import jakarta.el.BeanELResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.ListELResolver;
import jakarta.el.MapELResolver;
import jakarta.el.ResourceBundleELResolver;
import jakarta.el.VariableMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;

import com.sun.el.lang.FunctionMapperImpl;
import com.sun.el.lang.VariableMapperImpl;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@Dependent
public class ContextProducer {

    @Inject
    private BeanManager beanManager;

    @Produces
    @RequestScoped
    public ExtendedELContext createELContext() {
        ConstantResolver constantResolver = new ConstantResolver();

        CompositeELResolver resolver = createELResolver(constantResolver);

        return createELContext(resolver, new FunctionMapperImpl(), new VariableMapperImpl(), constantResolver);
    }

    private CompositeELResolver createELResolver(ConstantResolver constantResolver) {
        CompositeELResolver resolver = new CompositeELResolver();
        resolver.add(constantResolver);
        resolver.add(beanManager.getELResolver());

        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            resolver.add(facesContext.getELContext().getELResolver());
        }

        resolver.add(new MapELResolver());
        resolver.add(new ListELResolver());
        resolver.add(new ArrayELResolver());
        resolver.add(new ResourceBundleELResolver());
        resolver.add(new BeanELResolver());

        return resolver;
    }

    private ExtendedELContext createELContext(final ELResolver resolver, final FunctionMapper functionMapper, final VariableMapper variableMapper,
            final ConstantResolver constantResolver) {
        return new ExtendedELContext() {
            @Override
            public ELResolver getELResolver() {
                return resolver;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return functionMapper;
            }

            @Override
            public VariableMapper getVariableMapper() {
                return variableMapper;
            }

            @Override
            public ConstantResolver getConstantResolver() {
                return constantResolver;
            }
        };
    }

}
