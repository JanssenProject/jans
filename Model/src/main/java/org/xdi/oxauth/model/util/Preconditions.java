package org.xdi.oxauth.model.util;

/**
 * Dummy copy of guava, just to avoid dependency on guava!
 * TODO : remove once we will add guava to project
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/03/2016
 */

public class Preconditions {

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
