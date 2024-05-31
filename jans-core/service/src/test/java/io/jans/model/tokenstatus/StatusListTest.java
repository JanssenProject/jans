package io.jans.model.tokenstatus;

import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Z
 */
public class StatusListTest {

    @Test
    public void get_withStatusSize1_shouldReturnCorrectSetValue() {
        StatusList statusList = new StatusList( 1);
        statusList.set(0, 1);
        statusList.set(1, 0);
        statusList.set(2, 0);
        statusList.set(3, 1);
        statusList.set(4, 1);
        statusList.set(5, 1);
        statusList.set(6, 0);
        statusList.set(7, 1);
        statusList.set(8, 1);
        statusList.set(9, 1);
        statusList.set(10, 0);
        statusList.set(11, 0);
        statusList.set(12, 0);
        statusList.set(13, 1);
        statusList.set(14, 0);
        statusList.set(15, 1);

        assertEquals(1, statusList.get(0));
        assertEquals(0, statusList.get(1));
        assertEquals(0, statusList.get(2));
        assertEquals(1, statusList.get(3));
        assertEquals(1, statusList.get(4));
        assertEquals(1, statusList.get(5));
        assertEquals(0, statusList.get(6));
        assertEquals(1, statusList.get(7));
        assertEquals(1, statusList.get(8));
        assertEquals(1, statusList.get(9));
        assertEquals(0, statusList.get(10));
        assertEquals(0, statusList.get(11));
        assertEquals(0, statusList.get(12));
        assertEquals(1, statusList.get(13));
        assertEquals(0, statusList.get(14));
        assertEquals(1, statusList.get(15));
    }

    @Test
    public void getLst_withStatusSize1_shouldReturnCorrectValue() throws IOException {
        StatusList statusList = new StatusList( 1);
        statusList.set(0, 1);
        statusList.set(1, 0);
        statusList.set(2, 0);
        statusList.set(3, 1);
        statusList.set(4, 1);
        statusList.set(5, 1);
        statusList.set(6, 0);
        statusList.set(7, 1);
        statusList.set(8, 1);
        statusList.set(9, 1);
        statusList.set(10, 0);
        statusList.set(11, 0);
        statusList.set(12, 0);
        statusList.set(13, 1);
        statusList.set(14, 0);
        statusList.set(15, 1);

        assertEncodedValueAndPrintDecoded("eNrbuRgAAhcBXQ", statusList);
    }

    @Test
    public void getLst_withStatusSize2_shouldReturnCorrectValue() throws IOException {
        StatusList statusList = new StatusList( 2);
        statusList.set(0, 1);
        statusList.set(1, 2);
        statusList.set(2, 0);
        statusList.set(3, 3);
        statusList.set(4, 0);
        statusList.set(5, 1);
        statusList.set(6, 0);
        statusList.set(7, 1);
        statusList.set(8, 1);
        statusList.set(9, 2);
        statusList.set(10, 3);
        statusList.set(11, 3);

        assertEncodedValueAndPrintDecoded("eNo76fITAAPfAgc", statusList);
    }

    private static void assertEncodedValueAndPrintDecoded(String expectedEncodedValue, StatusList statusList) throws IOException {
        System.out.println("Decoded: " + statusList.toString());
        final String encoded = statusList.getLst();

        assertEquals(expectedEncodedValue, encoded);

        StatusList decodedList = StatusList.fromEncoded(encoded, statusList.getBits());
        System.out.println("Decoded List: " + decodedList.toString());
    }
}
