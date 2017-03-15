/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Static interface registry.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/05/2013
 */

public class InterfaceRegistry {

    private static Map<Class, Object> g_map = new HashMap<Class, Object>();

    private static volatile boolean checkInstanceOf = false;

    private InterfaceRegistry() {
    }

    public boolean isCheckWhetherInstanceOf() {
        return checkInstanceOf;
    }

    public void setCheckWhetherInstanceOf(boolean p_checkWhetherInstanceOf) {
        checkInstanceOf = p_checkWhetherInstanceOf;
    }

    /**
     * Retrieve an instance for the given interface.
     *
     * @param p_interface Interface to retreive the registered instance for.
     * @return NULL when the given interface is null or no instance was assigned to the given interface,
     *         instance of the given interface otherwise.
     */
    public static synchronized <T> T get(Class p_interface) {
        // Check parameter for null
        if (p_interface != null) {
            Object instance = g_map.get(p_interface);
            if (checkInstanceOf) {
                if (p_interface.isInstance(instance)) {
                    //noinspection unchecked
                    return (T) instance;
                }
            } else {
                return (T) instance;
            }
        }
        return null;
    }

    /**
     * Register an instance for an interface or remove the mapping. To remove the actual interface
     * mapping, simply provide null for the instance.
     *
     * @param p_interface Interface to (re-)create or remove a mapping to an instance for.
     * @param p_instance  Instance to map to the given interface. Provide null to remove the actual mapping.
     * @return FALSE, when the given interface is null or the instance is not of the interface type, TRUE otherwise.
     */
    public static synchronized boolean put(Class p_interface, Object p_instance) {
        boolean result = false;
        // Check if interface is not null and if the given class is really an interface, do nothing otherwise
        if ((p_interface != null) && (p_interface.isInterface())) {
            // Check whether the instance is null
            if (p_instance == null) {
                // Instance is null, remove it from the list if contained
                g_map.remove(p_interface);
                result = true;
            } else {
                if (checkInstanceOf) {
                    // Check if the instance is really of the type of the given interface
                    if (p_interface.isInstance(p_instance)) {
                        // (Re)Define the instance for the interface
                        g_map.put(p_interface, p_instance);
                        result = true;
                    }
                } else {
                    g_map.put(p_interface, p_instance);
                    result = true;
                }
            }
        }
        return result;
    }
}