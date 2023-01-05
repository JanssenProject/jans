package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ByteUtilTest extends BaseTest {

    @Test
    public void twoBytesAsInt_validBytes_correctIntValue() {
        showTitle("twoBytesAsInt_validBytes_correctIntValue");
        byte val1 = 1;
        byte val2 = 2;
        int result = ByteUtils.twoBytesAsInt(val1, val2);
        assertEquals(result, 258, "twoBytesAsInt fail to convert to Int, values: " + val1 + " , " + val2);
    }

    @Test
    public void twoIntsAsInt_validIntegers_correctIntValue() {
        showTitle("twoIntsAsInt_validIntegers_correctIntValue");
        int valueInt1 = 1;
        int valueInt2 = 2;
        int result = ByteUtils.twoIntsAsInt(valueInt1, valueInt2);
        assertEquals(result, 258, "twoIntsAsInt fail to convert to Int, values: " + valueInt1 + " , " + valueInt2);
    }

    @Test
    public void byteAsInt_validByte_correctIntValue() {
        showTitle("byteAsInt_validByte_correctIntValue");
        byte valueByte = 21;
        int result = ByteUtils.byteAsInt(valueByte);
        System.out.println(result);
        assertEquals(result, 21, "twoBytesAsInt fail to convert to Int, values: " + valueByte);
    }
}
