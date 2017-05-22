package org.xdi.service.el;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.el.VariableMapper;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;

import com.sun.el.lang.FunctionMapperImpl;
import com.sun.el.lang.VariableMapperImpl;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
public class ContextProducer {

	@Produces
	public  ELContext createELContext(BeanManager beanManager) {
		return createELContext(createELResolver(beanManager), new FunctionMapperImpl(), new VariableMapperImpl());
	}

	private ELResolver createELResolver(BeanManager beanManager) {
		CompositeELResolver resolver = new CompositeELResolver();
		resolver.add(beanManager.getELResolver());
		resolver.add(new MapELResolver());
		resolver.add(new ListELResolver());
		resolver.add(new ArrayELResolver());
		resolver.add(new ResourceBundleELResolver());
		resolver.add(new BeanELResolver());

		return resolver;
	}

	private ELContext createELContext(final ELResolver resolver, final FunctionMapper functionMapper,
			final VariableMapper variableMapper) {
		return new ELContext() {
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
		};
	}
}