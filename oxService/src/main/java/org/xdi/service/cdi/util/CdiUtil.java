/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.service.cdi.util;

import java.lang.reflect.Type;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan
 * @version 1.0, 05/05/2017
 */
public class CdiUtil {

    private final static Logger log = LoggerFactory.getLogger(CdiUtil.class);

    private CdiUtil() {
    }

    public static <T> T getContextBean(BeanManager beanManager, Type type, String beanName) {
		Bean<T> bean = (Bean<T>) beanManager.resolve(beanManager.getBeans(type, NamedLiteral.of(beanName)));
		if (bean == null) {
			return null;
		}

		T existingInstance = beanManager.getContext(bean.getScope()).get(bean, beanManager.createCreationalContext(bean));

    	return existingInstance;
	}
/*
    @Deprecated
    public static <T> T getContextualReference(BeanManager bm, Set<Bean<?>> beans, Class<?> type) {
		if (beans == null || beans.size() == 0) {
			return null;
		}

		// If we would resolve to multiple beans then BeanManager#resolve would throw an AmbiguousResolutionException
		Bean<?> bean = bm.resolve(beans);
		if (bean == null) {
			return null;
		} else {
			CreationalContext<?> creationalContext = bm.createCreationalContext(bean);
			return (T) bm.getReference(bean, type, creationalContext);
		}
	}
*/
    public static <T> Instance<T> instance(Class<T> p_clazz) {
		return CDI.current().select(p_clazz);
    }    	

    public static <T> Instance<T> instance(Class<T> p_clazz, String name) {
		return CDI.current().select(p_clazz, NamedLiteral.of(name));
    }    	

    public static <T> T bean(Class<T> p_clazz) {
    	return instance(p_clazz).get();
    }    	

    public static <T> T bean(Class<T> p_clazz, String name) {
		return instance(p_clazz, name).get();
    }    	

    public static <T> void destroy(Class<T> p_clazz) {
		Instance<T> instance = instance(p_clazz);
		if (instance.isResolvable()) {
			instance.destroy(instance.get());
		}
    }    	

    public static <T> T destroy(Class<T> p_clazz, String name) {
		Instance<T> instance = instance(p_clazz, name);
		if (instance.isResolvable()) {
			T obj = instance.get();
			instance.destroy(obj);
			
			return obj;
		}

		return null;
    }    	

}
