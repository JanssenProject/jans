/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

import java.util.Collection;

/**
 * Common assertion utilities
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public final class AssertionHelper {

    private AssertionHelper() {
    }

    /**
     * Check whether the object is null or not. If it is, throw an exception and
     * display the message.
     *
     * @param object
     *            the object to check.
     * @param message
     *            the message to display if the object is null.
     */
    public static void assertNotNull(final Object object, final String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Check whether the collection is null or empty. If it is, throw an exception
     * and display the message.
     *
     * @param list
     *            the collecion to check.
     * @param message
     *            the message to display if the object is null.
     */
    public static void assertNotEmpty(final Collection<?> list, final String message) {
        assertNotNull(list, message);
        if (list.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that the statement is true, otherwise throw an exception with the
     * provided message.
     *
     * @param condition
     *            the condition to assert is true.
     * @param message
     *            the message to display if the condition is not true.
     */
    public static void assertTrue(final boolean condition, final String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

}
