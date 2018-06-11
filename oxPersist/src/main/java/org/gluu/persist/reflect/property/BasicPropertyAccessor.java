/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.reflect.property;

import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.gluu.persist.exception.BasePersistenceException;
import org.gluu.persist.exception.PropertyAccessException;
import org.gluu.persist.exception.PropertyNotFoundException;
import org.gluu.persist.reflect.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accesses property values via a get/set pair, which may be nonpublic. The
 * default (and recommended strategy).
 */
public class BasicPropertyAccessor implements PropertyAccessor {

    private static final Logger LOG = LoggerFactory.getLogger(BasicPropertyAccessor.class);

    public static final class BasicSetter implements Setter {

        private static final long serialVersionUID = 1660638549257218450L;

        private Class<?> clazz;
        private final transient Method method;
        private final String propertyName;

        private BasicSetter(Class<?> clazz, Method method, String propertyName) {
            this.clazz = clazz;
            this.method = method;
            this.propertyName = propertyName;
        }

        public void set(Object target, Object value) throws BasePersistenceException {
            try {
                method.invoke(target, new Object[] {value});
            } catch (NullPointerException npe) {
                if (value == null && method.getParameterTypes()[0].isPrimitive()) {
                    throw new PropertyAccessException(npe, "Null value was assigned to a property of primitive type", true, clazz,
                            propertyName);
                } else {
                    throw new PropertyAccessException(npe, "NullPointerException occurred while calling", true, clazz, propertyName);
                }
            } catch (InvocationTargetException ite) {
                throw new PropertyAccessException(ite, "Exception occurred inside", true, clazz, propertyName);
            } catch (IllegalAccessException iae) {
                throw new PropertyAccessException(iae, "IllegalAccessException occurred while calling", true, clazz, propertyName);
                // cannot occur
            } catch (IllegalArgumentException iae) {
                if (value == null && method.getParameterTypes()[0].isPrimitive()) {
                    throw new PropertyAccessException(iae, "Null value was assigned to a property of primitive type", true, clazz,
                            propertyName);
                } else {
                    LOG.error("IllegalArgumentException in class: " + clazz.getName() + ", setter method of property: " + propertyName);
                    LOG.error("expected type: " + method.getParameterTypes()[0].getName() + ", actual value: "
                            + (value == null ? null : value.getClass().getName()));
                    throw new PropertyAccessException(iae, "IllegalArgumentException occurred while calling", true, clazz, propertyName);
                }
            }
        }

        public Method getMethod() {
            return method;
        }

        public String getMethodName() {
            return method.getName();
        }

        Object readResolve() {
            return createSetter(clazz, propertyName);
        }

        @Override
        public String toString() {
            return "BasicSetter(" + clazz.getName() + '.' + propertyName + ')';
        }
    }

    public static final class BasicGetter implements Getter {

        private static final long serialVersionUID = -5736635201315368096L;

        private Class<?> clazz;
        private final transient Method method;
        private final String propertyName;

        private BasicGetter(Class<?> clazz, Method method, String propertyName) {
            this.clazz = clazz;
            this.method = method;
            this.propertyName = propertyName;
        }

        public Object get(Object target) throws BasePersistenceException {
            try {
                return method.invoke(target, (Object[]) null);
            } catch (InvocationTargetException ite) {
                throw new PropertyAccessException(ite, "Exception occurred inside", false, clazz, propertyName);
            } catch (IllegalAccessException iae) {
                throw new PropertyAccessException(iae, "IllegalAccessException occurred while calling", false, clazz, propertyName);
                // cannot occur
            } catch (IllegalArgumentException iae) {
                LOG.error("IllegalArgumentException in class: " + clazz.getName() + ", getter method of property: " + propertyName);
                throw new PropertyAccessException(iae, "IllegalArgumentException occurred calling", false, clazz, propertyName);
            }
        }

        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        public Method getMethod() {
            return method;
        }

        public String getMethodName() {
            return method.getName();
        }

        @Override
        public String toString() {
            return "BasicGetter(" + clazz.getName() + '.' + propertyName + ')';
        }

        Object readResolve() {
            return createGetter(clazz, propertyName);
        }
    }

    public Setter getSetter(Class<?> theClass, String propertyName) throws PropertyNotFoundException {
        return createSetter(theClass, propertyName);
    }

    private static Setter createSetter(Class<?> theClass, String propertyName) throws PropertyNotFoundException {
        BasicSetter result = getSetterOrNull(theClass, propertyName);
        if (result == null) {
            throw new PropertyNotFoundException("Could not find a setter for property " + propertyName + " in class " + theClass.getName());
        }
        return result;
    }

    private static BasicSetter getSetterOrNull(Class<?> theClass, String propertyName) {
        if ((theClass == Object.class) || (theClass == null)) {
            return null;
        }

        Method method = setterMethod(theClass, propertyName);

        if (method != null) {
            if (!ReflectHelper.isPublic(theClass, method)) {
                method.setAccessible(true);
            }

            return new BasicSetter(theClass, method, propertyName);
        } else {
            BasicSetter setter = getSetterOrNull(theClass.getSuperclass(), propertyName);
            if (setter == null) {
                Class<?>[] interfaces = theClass.getInterfaces();
                for (int i = 0; setter == null && i < interfaces.length; i++) {
                    setter = getSetterOrNull(interfaces[i], propertyName);
                }
            }
            return setter;
        }

    }

    private static Method setterMethod(Class<?> theClass, String propertyName) {

        BasicGetter getter = getGetterOrNull(theClass, propertyName);
        Class<?> returnType = (getter == null) ? null : getter.getReturnType();

        Method[] methods = theClass.getDeclaredMethods();
        Method potentialSetter = null;
        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();

            if (methods[i].getParameterTypes().length == 1 && methodName.startsWith("set")) {
                String testStdMethod = Introspector.decapitalize(methodName.substring(3));
                String testOldMethod = methodName.substring(3);
                if (testStdMethod.equals(propertyName) || testOldMethod.equals(propertyName)) {
                    potentialSetter = methods[i];
                    if (returnType == null || methods[i].getParameterTypes()[0].equals(returnType)) {
                        return potentialSetter;
                    }
                }
            }
        }
        return potentialSetter;
    }

    public Getter getGetter(Class<?> theClass, String propertyName) throws PropertyNotFoundException {
        return createGetter(theClass, propertyName);
    }

    public static Getter createGetter(Class<?> theClass, String propertyName) throws PropertyNotFoundException {
        BasicGetter result = getGetterOrNull(theClass, propertyName);
        if (result == null) {
            throw new PropertyNotFoundException("Could not find a getter for " + propertyName + " in class " + theClass.getName());
        }
        return result;

    }

    private static BasicGetter getGetterOrNull(Class<?> theClass, String propertyName) {
        if ((theClass == Object.class) || (theClass == null)) {
            return null;
        }

        Method method = getterMethod(theClass, propertyName);

        if (method != null) {
            if (!ReflectHelper.isPublic(theClass, method)) {
                method.setAccessible(true);
            }

            return new BasicGetter(theClass, method, propertyName);
        } else {
            BasicGetter getter = getGetterOrNull(theClass.getSuperclass(), propertyName);
            if (getter == null) {
                Class<?>[] interfaces = theClass.getInterfaces();
                for (int i = 0; getter == null && i < interfaces.length; i++) {
                    getter = getGetterOrNull(interfaces[i], propertyName);
                }
            }
            return getter;
        }
    }

    private static Method getterMethod(Class<?> theClass, String propertyName) {

        Method[] methods = theClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            // only carry on if the method has no parameters
            if (methods[i].getParameterTypes().length == 0) {
                String methodName = methods[i].getName();

                // try "get"
                if (methodName.startsWith("get")) {
                    String testStdMethod = Introspector.decapitalize(methodName.substring(3));
                    String testOldMethod = methodName.substring(3);
                    if (testStdMethod.equals(propertyName) || testOldMethod.equals(propertyName)) {
                        return methods[i];
                    }

                }

                // if not "get", then try "is"
                if (methodName.startsWith("is")) {
                    String testStdMethod = Introspector.decapitalize(methodName.substring(2));
                    String testOldMethod = methodName.substring(2);
                    if (testStdMethod.equals(propertyName) || testOldMethod.equals(propertyName)) {
                        return methods[i];
                    }
                }
            }
        }
        return null;
    }

}
