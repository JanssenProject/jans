package io.jans.orm.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/**
 * @author Yuriy Z
 */
public class StringHelperTest {

    @Test
    public void isEmptyString_forNull_shouldReturnTrue() {
        assertTrue(StringHelper.isEmptyString(null));
    }

    @Test
    public void isEmptyString_forEmptyString_shouldReturnTrue() {
        assertTrue(StringHelper.isEmptyString(""));
    }

    @Test
    public void isEmptyString_forNonEmptyString_shouldReturnFalse() {
        assertFalse(StringHelper.isEmptyString("df"));
    }

}
