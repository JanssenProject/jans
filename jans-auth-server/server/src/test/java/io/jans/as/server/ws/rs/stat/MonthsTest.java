package io.jans.as.server.ws.rs.stat;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class MonthsTest {

    @Test
    public void getMonths_forValidStartAndEndDates_shouldReturnCorrectResult() {
        final Set<String> months = Months.getMonths("202112", "202205");
        assertEquals(months.size(), 6);
        assertTrue(months.containsAll(Arrays.asList("202112", "202201", "202202", "202203", "202204", "202205")));
    }

    @Test
    public void getMonths_whenStartIsAfterEndDates_shouldReturnEmptyList() {
        final Set<String> months = Months.getMonths("202212", "202205");
        assertTrue(months.isEmpty());
    }

    @Test
    public void isValid_checkDifferentCases() {
        assertTrue(Months.isValid(null, "202112", "202205"));
        assertTrue(Months.isValid("", "202112", "202205"));
        assertTrue(Months.isValid("", "202012", "202205"));
        assertTrue(Months.isValid("202012", "", ""));
        assertTrue(Months.isValid("202012", null, null));
        assertTrue(Months.isValid("202012 202101", null, null));

        assertFalse(Months.isValid(null, null, null));
        assertFalse(Months.isValid("", "", ""));
        assertFalse(Months.isValid("202012 202101", "202012", "202205"));
    }
}
