/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cdi.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan
 * @version 1.0, 05/05/2017
 */
public final class CdiUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CdiUtil.class);

    private CdiUtil() {
    }

    public static <T> T getContextBean(BeanManager beanManager, Type type, String beanName, Annotation... qualifiers) {
        NamedLiteral namedLiteral = NamedLiteral.of(beanName);

        Annotation[] allQualifiers = null;
        if (qualifiers == null) {
            allQualifiers = new Annotation[] { namedLiteral };
        } else {
            allQualifiers = (Annotation[]) ArrayUtils.add(qualifiers, namedLiteral);
        }

        Bean<T> bean = (Bean<T>) beanManager.resolve(beanManager.getBeans(type, qualifiers));
		if (bean == null) {
			return null;
		}

        T existingInstance = beanManager.getContext(bean.getScope()).get(bean, beanManager.createCreationalContext(bean));

        return existingInstance;
    }

    /*
     * @Deprecated public static <T> T getContextualReference(BeanManager bm,
     * Set<Bean<?>> beans, Class<?> type) { if (beans == null || beans.size() == 0)
     * { return null; }
     *
     * // If we would resolve to multiple beans then BeanManager#resolve would throw
     * an AmbiguousResolutionException Bean<?> bean = bm.resolve(beans); if (bean ==
     * null) { return null; } else { CreationalContext<?> creationalContext =
     * bm.createCreationalContext(bean); return (T) bm.getReference(bean, type,
     * creationalContext); } }
     */
    public static <T> Instance<T> instance(Class<T> clazz) {
        return CDI.current().select(clazz);
    }

    public static <T> Instance<T> instance(Class<T> clazz, String name) {
        return CDI.current().select(clazz, NamedLiteral.of(name));
    }

    public static <T> T bean(Class<T> clazz) {
        return instance(clazz).get();
    }

    public static <T> T bean(Class<T> clazz, String name) {
        return instance(clazz, name).get();
    }

    public static <T> void destroy(Class<T> clazz) {
        Instance<T> instance = instance(clazz);
        if (instance.isResolvable()) {
            instance.destroy(instance.get());
        }
    }

    public static <T> T destroy(Class<T> clazz, String name) {
        Instance<T> instance = instance(clazz, name);
        if (instance.isResolvable()) {
            T obj = instance.get();
            instance.destroy(obj);

            return obj;
        }

        return null;
    }

}
