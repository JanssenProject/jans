/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.reflect.property;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gluu.persist.exception.PropertyNotFoundException;
import org.gluu.persist.reflect.util.ReflectHelper;
import org.gluu.util.StringHelper;

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
            Field[] fileds = thisClass.getDeclaredFields();
            for (Field filed : fileds) {
                List<Annotation> annotations = getOnlyAllowedAnntotations(filed.getAnnotations(), allowedAnnotations);
                if ((annotations == null) || (annotations.size() == 0)) {
                    continue;
                }

                result.put(filed.getName(), annotations);
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
