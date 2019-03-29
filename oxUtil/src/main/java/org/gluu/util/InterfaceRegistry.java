/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Static interface registry.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/05/2013
 */

public final class InterfaceRegistry {

    private static Map<Class, Object> REGISTRY = new HashMap<Class, Object>();

    private static volatile boolean CHECK_INSTANCE_OF = false;

    private InterfaceRegistry() { }

    public boolean isCheckWhetherInstanceOf() {
        return CHECK_INSTANCE_OF;
    }

    public static void setCheckWhetherInstanceOf(boolean checkInstanceOf) {
        CHECK_INSTANCE_OF = checkInstanceOf;
    }

    /**
     * Retrieve an instance for the given interface.
     *
     * @param classInterface
     *            Interface to retreive the registered instance for.
     * @return NULL when the given interface is null or no instance was assigned to
     *         the given interface, instance of the given interface otherwise.
     */
    public static synchronized <T> T get(Class classInterface) {
        // Check parameter for null
        if (classInterface != null) {
            Object instance = REGISTRY.get(classInterface);
            if (CHECK_INSTANCE_OF) {
                if (classInterface.isInstance(instance)) {
                    // noinspection unchecked
                    return (T) instance;
                }
            } else {
                return (T) instance;
            }
        }
        return null;
    }

    /**
     * Register an instance for an interface or remove the mapping. To remove the
     * actual interface mapping, simply provide null for the instance.
     *
     * @param classInterface
     *            Interface to (re-)create or remove a mapping to an instance for.
     * @param classInstance
     *            Instance to map to the given interface. Provide null to remove the
     *            actual mapping.
     * @return FALSE, when the given interface is null or the instance is not of the
     *         interface type, TRUE otherwise.
     */
    public static synchronized boolean put(Class classInterface, Object classInstance) {
        boolean result = false;
        // Check if interface is not null and if the given class is really an interface,
        // do nothing otherwise
        if ((classInterface != null) && (classInterface.isInterface())) {
            // Check whether the instance is null
            if (classInstance == null) {
                // Instance is null, remove it from the list if contained
                REGISTRY.remove(classInterface);
                result = true;
            } else {
                if (CHECK_INSTANCE_OF) {
                    // Check if the instance is really of the type of the given interface
                    if (classInterface.isInstance(classInstance)) {
                        // (Re)Define the instance for the interface
                        REGISTRY.put(classInterface, classInstance);
                        result = true;
                    }
                } else {
                    REGISTRY.put(classInterface, classInstance);
                    result = true;
                }
            }
        }
        return result;
    }
}
