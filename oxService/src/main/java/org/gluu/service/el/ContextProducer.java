package org.gluu.service.el;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.el.VariableMapper;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

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
