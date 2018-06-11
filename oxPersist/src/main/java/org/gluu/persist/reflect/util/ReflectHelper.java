/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.reflect.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gluu.persist.exception.MappingException;
import org.gluu.persist.exception.PropertyNotFoundException;
import org.gluu.persist.reflect.property.BasicPropertyAccessor;
import org.gluu.persist.reflect.property.BasicPropertyAnnotationResolver;
import org.gluu.persist.reflect.property.DirectPropertyAccessor;
import org.gluu.persist.reflect.property.Getter;
import org.gluu.persist.reflect.property.PropertyAccessor;
import org.gluu.persist.reflect.property.Setter;

/**
 * Utility class for various reflection operations.
 */
public final class ReflectHelper {

    private static final PropertyAccessor BASIC_PROPERTY_ACCESSOR = new BasicPropertyAccessor();
    private static final PropertyAccessor DIRECT_PROPERTY_ACCESSOR = new DirectPropertyAccessor();

    private static final BasicPropertyAnnotationResolver BASIC_PROPERTY_ANNOTATION_RESOLVER = new BasicPropertyAnnotationResolver();

    public static final Class<?>[] NO_PARAM_SIGNATURE = new Class[0];
    public static final Object[] NO_PARAMS = new Object[0];
    public static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    // findbugs: mutable static field should be package protected. In case it needed outside of the class please copy via method
    private static final Class<?>[] SINGLE_OBJECT_PARAM_SIGNATURE = new Class<?>[] {Object.class};

    private static final Method OBJECT_EQUALS;
    private static final Method OBJECT_HASHCODE;

    static {
        Method eq;
        Method hash;
        try {
            eq = extractEqualsMethod(Object.class);
            hash = extractHashCodeMethod(Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not find Object.equals() or Object.hashCode()", e);
        }
        OBJECT_EQUALS = eq;
        OBJECT_HASHCODE = hash;
    }

    /**
     * Disallow instantiation of ReflectHelper.
     */
    private ReflectHelper() {
    }

    /**
     * Encapsulation of getting hold of a class's {@link Object#equals equals}
     * method.
     *
     * @param clazz
     *            The class from which to extract the equals method.
     * @return The equals method reference
     * @throws NoSuchMethodException
     *             Should indicate an attempt to extract equals method from
     *             interface.
     */
    public static Method extractEqualsMethod(Class<?> clazz) throws NoSuchMethodException {
        return clazz.getMethod("equals", SINGLE_OBJECT_PARAM_SIGNATURE);
    }

    /**
     * Encapsulation of getting hold of a class's {@link Object#hashCode
     * hashCode} method.
     *
     * @param clazz
     *            The class from which to extract the hashCode method.
     * @return The hashCode method reference
     * @throws NoSuchMethodException
     *             Should indicate an attempt to extract hashCode method from
     *             interface.
     */
    public static Method extractHashCodeMethod(Class<?> clazz) throws NoSuchMethodException {
        return clazz.getMethod("hashCode", NO_PARAM_SIGNATURE);
    }

    /**
     * Determine if the given class defines an {@link Object#equals} override.
     *
     * @param clazz
     *            The class to check
     * @return True if clazz defines an equals override.
     */
    public static boolean overridesEquals(Class<?> clazz) {
        Method equals;
        try {
            equals = extractEqualsMethod(clazz);
        } catch (NoSuchMethodException nsme) {
            return false; // its an interface so we can't really tell
            // anything...
        }
        return !OBJECT_EQUALS.equals(equals);
    }

    /**
     * Determine if the given class defines a {@link Object#hashCode} override.
     *
     * @param clazz
     *            The class to check
     * @return True if clazz defines an hashCode override.
     */
    public static boolean overridesHashCode(Class<?> clazz) {
        Method hashCode;
        try {
            hashCode = extractHashCodeMethod(clazz);
        } catch (NoSuchMethodException nsme) {
            return false; // its an interface so we can't really tell
            // anything...
        }
        return !OBJECT_HASHCODE.equals(hashCode);
    }

    /**
     * Determine if the given class implements or extend the given class.
     *
     * @param clazz
     *            The class to check
     * @param intf
     *            The interface to check it against.
     * @return True if the class does implement the interface, false otherwise.
     */
    public static boolean assignableFrom(Class<?> clazz, Class<?> intf) {
        return intf.isAssignableFrom(clazz);
    }

    /**
     * Perform resolution of a class name.
     * <p/>
     * Here we first check the context classloader, if one, before delegating to
     * {@link Class#forName(String, boolean, ClassLoader)} using the caller's
     * classloader
     *
     * @param name
     *            The class name
     * @param caller
     *            The class from which this call originated (in order to access
     *            that class's loader).
     * @return The class reference.
     * @throws ClassNotFoundException
     *             From {@link Class#forName(String, boolean, ClassLoader)}.
     */
    public static Class<?> classForName(String name, Class<?> caller) throws ClassNotFoundException {
        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                return contextClassLoader.loadClass(name);
            }
        } catch (Throwable ignore) {
        }
        return Class.forName(name, true, caller.getClassLoader());
    }

    /**
     * Perform resolution of a class name.
     * <p/>
     * Same as {@link #classForName(String, Class)} except that here we delegate
     * to {@link Class#forName(String)} if the context classloader lookup is
     * unsuccessful.
     *
     * @param name
     *            The class name
     * @return The class reference.
     * @throws ClassNotFoundException
     *             From {@link Class#forName(String)}.
     */
    public static Class<?> classForName(String name) throws ClassNotFoundException {
        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                return contextClassLoader.loadClass(name);
            }
        } catch (Throwable ignore) {
        }
        return Class.forName(name);
    }

    /**
     * Is this member publicly accessible.
     * <p/>
     * Short-hand for {@link #isPublic(Class, Member)} passing the member +
     * {@link Member#getDeclaringClass()}
     *
     * @param member
     *            The member to check
     * @return True if the member is publicly accessible.
     */
    public static boolean isPublic(Member member) {
        return isPublic(member.getDeclaringClass(), member);
    }

    /**
     * Is this member publicly accessible.
     *
     * @param clazz
     *            The class which defines the member
     * @param member
     *            The memeber.
     * @return True if the member is publicly accessible, false otherwise.
     */
    public static boolean isPublic(Class<?> clazz, Member member) {
        return Modifier.isPublic(member.getModifiers()) && Modifier.isPublic(clazz.getModifiers());
    }

    /**
     * Attempt to resolve the specified property type through reflection.
     *
     * @param className
     *            The name of the class owning the property.
     * @param name
     *            The name of the property.
     * @return The type of the property.
     * @throws MappingException
     *             Indicates we were unable to locate the property.
     */
    public static Class<?> reflectedPropertyClass(String className, String name) throws MappingException {
        try {
            Class<?> clazz = ReflectHelper.classForName(className);
            return getter(clazz, name).getReturnType();
        } catch (ClassNotFoundException cnfe) {
            throw new MappingException("class " + className + " not found while looking for property: " + name, cnfe);
        }
    }

    private static Getter getter(Class<?> clazz, String name) throws MappingException {
        try {
            return BASIC_PROPERTY_ACCESSOR.getGetter(clazz, name);
        } catch (PropertyNotFoundException pnfe) {
            return DIRECT_PROPERTY_ACCESSOR.getGetter(clazz, name);
        }
    }

    private static Setter setter(Class<?> clazz, String name) throws MappingException {
        try {
            return BASIC_PROPERTY_ACCESSOR.getSetter(clazz, name);
        } catch (PropertyNotFoundException pnfe) {
            return DIRECT_PROPERTY_ACCESSOR.getSetter(clazz, name);
        }
    }

    /**
     * Directly retrieve the {@link Getter} reference via the
     * {@link BasicPropertyAccessor}.
     *
     * @param theClass
     *            The class owning the property
     * @param name
     *            The name of the property
     * @return The getter.
     * @throws MappingException
     *             Indicates we were unable to locate the property.
     */
    public static Getter getGetter(Class<?> theClass, String name) throws MappingException {
        return BASIC_PROPERTY_ACCESSOR.getGetter(theClass, name);
    }

    public static Getter getMethodOrPropertyGetter(Class<?> theClass, String name) throws MappingException {
        return getter(theClass, name);
    }

    /**
     * Directly retrieve the {@link Setter} reference via the
     * {@link BasicPropertyAccessor}.
     *
     * @param theClass
     *            The class owning the property
     * @param name
     *            The name of the property
     * @return The setter.
     * @throws MappingException
     *             Indicates we were unable to locate the property.
     */
    public static Setter getSetter(Class<?> theClass, String name) throws MappingException {
        return BASIC_PROPERTY_ACCESSOR.getSetter(theClass, name);
    }

    public static Setter getMethodOrPropertySetter(Class<?> theClass, String name) throws MappingException {
        return setter(theClass, name);
    }

    /**
     * Retrieve the default (no arg) constructor from the given class.
     *
     * @param clazz
     *            The class for which to retrieve the default ctor.
     * @return The default constructor.
     * @throws PropertyNotFoundException
     *             Indicates there was not publicly accessible, no-arg
     *             constructor (todo : why PropertyNotFoundException???)
     */
    public static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) throws PropertyNotFoundException {
        if (isAbstractClass(clazz)) {
            return null;
        }

        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(NO_PARAM_SIGNATURE);
            if (!isPublic(clazz, constructor)) {
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (NoSuchMethodException nme) {
            throw new PropertyNotFoundException("Object class [" + clazz.getName() + "] must declare a default (no-argument) constructor");
        }
    }

    public static <T> T createObjectByDefaultConstructor(Class<T> clazz) throws PropertyNotFoundException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        return getDefaultConstructor(clazz).newInstance(NO_PARAMS);
    }

    /**
     * Determine if the given class is declared abstract.
     *
     * @param clazz
     *            The class to check.
     * @return True if the class is abstract, false otherwise.
     */
    public static boolean isAbstractClass(Class<?> clazz) {
        int modifier = clazz.getModifiers();
        return Modifier.isAbstract(modifier) || Modifier.isInterface(modifier);
    }

    /**
     * Determine is the given class is declared final.
     *
     * @param clazz
     *            The class to check.
     * @return True if the class is final, flase otherwise.
     */
    public static boolean isFinalClass(Class<?> clazz) {
        return Modifier.isFinal(clazz.getModifiers());
    }

    /**
     * Retrieve a constructor for the given class, with arguments matching the
     * specified Hibernate mapping.
     *
     * @param clazz
     *            The class needing instantiation
     * @param parameterTypes
     *            The types representing the required ctor param signature
     * @return The matching constructor.
     * @throws PropertyNotFoundException
     *             Indicates we could not locate an appropriate constructor
     *             (todo : again with PropertyNotFoundException???)
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) throws PropertyNotFoundException {
        Constructor<?> constructor = null;
        try {
            constructor = clazz.getConstructor(parameterTypes);
        } catch (Exception e) {
            throw new PropertyNotFoundException("Object class [" + clazz.getName() + "] must declare constructor with specifid types "
                    + Arrays.toString(parameterTypes));
        }

        if (constructor != null) {
            if (!isPublic(clazz, constructor)) {
                constructor.setAccessible(true);
            }
            return constructor;
        }

        throw new PropertyNotFoundException("no appropriate constructor in class: " + clazz.getName());
    }

    public static Method getMethod(Class<?> clazz, Method method) {
        try {
            return clazz.getMethod(method.getName(), method.getParameterTypes());
        } catch (Exception e) {
            return null;
        }
    }

    public static Annotation getAnnotationByType(List<Annotation> annotations, Class<?> annotationType) {
        if (annotations == null) {
            return null;
        }

        return getAnnotationByType(annotations.toArray(NO_ANNOTATIONS), annotationType);
    }

    public static Annotation getAnnotationByType(Annotation[] annotations, Class<?> annotationType) {

        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationType)) {
                return annotation;
            }
        }

        return null;
    }

    public static String getPropertyNameByType(Map<String, List<Annotation>> propertiesAnnotations, Class<?> annotationType) {
        for (Entry<String, List<Annotation>> propertiesAnnotation : propertiesAnnotations.entrySet()) {
            Annotation annotation = getAnnotationByType(propertiesAnnotation.getValue(), annotationType);
            if (annotation != null) {
                return propertiesAnnotation.getKey();
            }
        }

        return null;
    }

    public static boolean isNotPrimitiveClass(Class<?> theClass) {
        return !((theClass == null) || theClass.equals(Object.class) || theClass.isPrimitive());
    }

    public static List<Annotation> getClassAnnotations(Class<?> theClass, Class<?>... allowedAnnotations) throws PropertyNotFoundException {
        return BASIC_PROPERTY_ANNOTATION_RESOLVER.getClassAnnotations(theClass, allowedAnnotations);
    }

    public static List<Annotation> getPropertyAnnotations(Class<?> theClass, String propertyName, Class<?>... allowedAnnotations)
            throws PropertyNotFoundException {
        return BASIC_PROPERTY_ANNOTATION_RESOLVER.getPropertyAnnotations(theClass, propertyName, allowedAnnotations);
    }

    public static Map<String, List<Annotation>> getPropertiesAnnotations(Class<?> theClass, Class<?>... allowedAnnotations) {
        return BASIC_PROPERTY_ANNOTATION_RESOLVER.getPropertiesAnnotations(theClass, allowedAnnotations);
    }

    public static Class<?> getListType(Getter getter) {
        if (getter == null) {
            return null;
        }

        Type type = getter.getMethod().getGenericReturnType();
        if (assignableFrom(type.getClass(), ParameterizedType.class)) {
            return (Class<?>) (((ParameterizedType) type).getActualTypeArguments())[0];
        } else {
            return null;
        }
    }

    public static Class<?> getListType(Setter setter) {
        if (setter == null) {
            return null;
        }

        Type[] types = setter.getMethod().getGenericParameterTypes();
        if (assignableFrom(ParameterizedType[].class, types.getClass())) {
            return (Class<?>) ((ParameterizedType) types[0]).getActualTypeArguments()[0];
        } else {
            return null;
        }
    }

    public static Class<?> getSetterType(Setter setter) {
        if (setter == null) {
            return null;
        }

        return setter.getMethod().getParameterTypes()[0];
    }

    public static Object getValue(Object object, String name) throws MappingException {
        if (object == null) {
            throw new MappingException("Input value is null");
        }

        Getter getter = BASIC_PROPERTY_ACCESSOR.getGetter(object.getClass(), name);

        return getter.get(object);
    }

    public static Object createArray(Class<?> clazz, int length) {
        if (clazz.isArray()) {
            return Array.newInstance(clazz.getComponentType(), length);
        } else {
            return Array.newInstance(clazz, length);
        }
    }

    public static Object getPropertyValue(Object entry, Getter[] propertyGetters) {
        if ((entry == null) || (propertyGetters.length == 0)) {
            return null;
        }

        Object curEntry = entry;
        for (Getter propertyGetter : propertyGetters) {
            if (curEntry == null) {
                break;
            }
            curEntry = propertyGetter.get(curEntry);
        }

        return curEntry;
    }

    public static void copyObjectPropertyValues(Object fromEntry, Object toEntry, Getter[] propertyGetters, Setter[] propertySetters) {
        if ((fromEntry == null) || (toEntry == null) || (propertyGetters.length == 0)) {
            return;
        }

        if (propertyGetters.length != propertySetters.length) {
            throw new MappingException("Invalid numbers of setters specified");
        }

        for (int i = 0; i < propertyGetters.length; i++) {
            Object value = propertyGetters[i].get(fromEntry);
            propertySetters[i].set(toEntry, value);
        }
    }

    public static void sumObjectPropertyValues(Object resultEntry, Object entryToAdd, Getter[] propertyGetters, Setter[] propertySetters) {
        if ((resultEntry == null) || (entryToAdd == null) || (propertyGetters.length == 0)) {
            return;
        }

        if (propertyGetters.length != propertySetters.length) {
            throw new MappingException("Invalid numbers of setters specified");
        }

        for (int i = 0; i < propertyGetters.length; i++) {
            Class<?> returnType = propertyGetters[i].getReturnType();
            Object value1 = propertyGetters[i].get(resultEntry);
            Object value2 = propertyGetters[i].get(entryToAdd);

            Object resultValue;
            if ((returnType == int.class) || (returnType == Integer.class)) {
                Integer num1 = (value1 == null) ? 0 : (Integer) value1;
                Integer num2 = (value2 == null) ? 0 : (Integer) value2;

                resultValue = (int) 0 + num1 + num2;
            } else if ((returnType == float.class) || (returnType == Float.class)) {
                Float num1 = (value1 == null) ? 0 : (Float) value1;
                Float num2 = (value2 == null) ? 0 : (Float) value2;

                resultValue = 0.0f + num1 + num2;
            } else if ((returnType == double.class) || (returnType == Double.class)) {
                Double num1 = (value1 == null) ? 0 : (Double) value1;
                Double num2 = (value2 == null) ? 0 : (Double) value2;

                resultValue = 0.0d + num1 + num2;
            } else {
                throw new MappingException("Invalid return type of method " + propertyGetters[i].getMethodName());
            }

            propertySetters[i].set(resultEntry, resultValue);
        }
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>[] methodParams) throws NoSuchMethodException {
        try {
            return clazz.getMethod(methodName, methodParams);
        } catch (Exception ex) {
            throw new NoSuchMethodException("Method " + methodName + " doesn't exist in class " + clazz);
        }
    }

}
