package org.xdi.oxd.server;

import org.apache.commons.lang.StringUtils;

import java.util.List;

import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class TestUtils {

    private TestUtils() {
    }

    public static void notEmpty(String str) {
        assertTrue(StringUtils.isNotBlank(str));
    }

    public static void notEmpty(List<String> str) {
        assertTrue(str != null && !str.isEmpty() && StringUtils.isNotBlank(str.get(0)));
    }
}
