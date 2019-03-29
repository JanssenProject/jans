/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

import java.util.Random;

/**
 * Generator for iNumber
 */
public final class INumGenerator {

    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final Random RANDOM = new Random();

    /**
     * Avoid instance creation.
     */
    private INumGenerator() {
    }

    @Deprecated
    public static String generate() {
        return fourRandomChars();
    }

    /**
     * Generates a random inum of given size.
     *
     * @param size
     *            - number of four-digit sequences to be generated.
     * @return inum in a form of 0123.4567.89AB.CDEF
     */

    public static String generate(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Inum size should be not less then 1.");
        }
        StringBuilder result = new StringBuilder(fourRandomChars());
        for (int i = 0; i < size - 1; i++) {
            result.append(".").append(fourRandomChars());
        }
        return result.toString();
    }

    public static String fourRandomChars() {
        final StringBuilder result = new StringBuilder();
        result.append(randomHexChar()).append(randomHexChar()).append(randomHexChar()).append(randomHexChar());
        return result.toString();
    }

    private static char randomHexChar() {
        return HEX_CHARS[RANDOM.nextInt(HEX_CHARS.length)];
    }

}
