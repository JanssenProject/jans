package io.jans.util;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Z
 */
public class IdUtilTest {

    @Test
    public void shortUuid_lenthMustBe22() {
        assertEquals(22, IdUtil.randomShortUUID().length());
    }

    @Test(enabled = false)
    public void shortUuid_generateALotIdsAndPrintThem() {
        for (int i = 0; i < 100000; i++) {
            final String shortUUID = IdUtil.randomShortUUID();
            System.out.println(shortUUID + "  length: " + shortUUID.length());
        };
    }
}
