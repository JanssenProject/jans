/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.reflect.property;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jans.orm.exception.PropertyNotFoundException;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.orm.util.StringHelper;

/**
 * Defines a strategy for accessing class and propery annotations.
 *
 * @author Yuriy Movchan Date: 10.08.2010
 */
public class BasicPropertyAnnotationResolver implements PropertyAnnotationResolver {

    public List<Annotation> getClassAnnotations(Class<?> theClass, Class<?>... allowedAnnotations) {
        List<Annotation> result = getOnlyAllowedAnntotations(theClass.getAnnotations(), allowedAnnotations);

        Class<?> superClass = theClass.getSuperclass();
        while (ReflectHelper.isNotPrimitiveClass(superClass)) {
            result.addAll(getOnlyAllowedAnntotations(superClass.getAnnotations(), allowedAnnotations));
            superClass = superClass.getSuperclass();
        }

        return result;
    }

    public List<Annotation> getPropertyAnnotations(Class<?> theClass, String propertyName, Class<?>... allowedAnnotations)
            throws PropertyNotFoundException {
        if (StringHelper.isEmpty(propertyName)) {
            throw new PropertyNotFoundException("Could not find property " + propertyName + " in class " + theClass.getName());
        }

        Class<?> thisClass = theClass;
        while (ReflectHelper.isNotPrimitiveClass(thisClass)) {
            Field[] fileds = thisClass.getDeclaredFields();
            for (Field filed : fileds) {
                if (propertyName.equals(filed.getName())) {
                    return getOnlyAllowedAnntotations(filed.getAnnotations(), allowedAnnotations);
                }
            }
            thisClass = thisClass.getSuperclass();
        }

        throw new PropertyNotFoundException("Could not find property " + propertyName + " in class " + theClass.getName());
    }

    public Map<String, List<Annotation>> getPropertiesAnnotations(Class<?> theClass, Class<?>... allowedAnnotations) {
        Map<String, List<Annotation>> result = new HashMap<String, List<Annotation>>();

        Class<?> thisClass = theClass;
        while (ReflectHelper.isNotPrimitiveClass(thisClass)) {
            Field[] fields = thisClass.getDeclaredFields();
            for (Field field : fields) {
                List<Annotation> annotations = getOnlyAllowedAnntotations(field.getAnnotations(), allowedAnnotations);
                if ((annotations == null) || (annotations.size() == 0)) {
                    continue;
                }

                result.put(field.getName(), annotations);
            }
            thisClass = thisClass.getSuperclass();
        }

        return result;
    }

    private List<Annotation> getOnlyAllowedAnntotations(Annotation[] annotations, Class<?>[] allowedAnnotations) {
        List<Annotation> result = new ArrayList<Annotation>();
        if (annotations.length == 0) {
            return result;
        }

        for (Annotation annotation : annotations) {
            for (Class<?> allowedAnnotation : allowedAnnotations) {
                if (annotation.annotationType().equals(allowedAnnotation)) {
                    result.add(annotation);
                }
            }
        }

        return result;
    }

}
