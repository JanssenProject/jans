package io.jans.model.token;

import io.jans.orm.annotation.AttributeName;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
public class AbstractIndexPoolTest {

    private static final long HOUR = 60 * 60 * 1000L;

    @Test
    public void nextIndex_whenIssueUntilInFutureAndExpirationInPast_shouldReturnIndex() {
        AbstractIndexPool pool = pool(0, 9);
        pool.setExpirationDate(new Date(System.currentTimeMillis() - HOUR));
        pool.setIssueUntil(new Date(System.currentTimeMillis() + HOUR));

        assertEquals(pool.nextIndex(), 0);
        assertEquals(pool.nextIndex(), 1);
    }

    @Test
    public void nextIndex_whenIssueUntilInPastAndExpirationInFuture_shouldReturnMinusOne() {
        AbstractIndexPool pool = pool(0, 9);
        pool.setExpirationDate(new Date(System.currentTimeMillis() + 24 * HOUR));
        pool.setIssueUntil(new Date(System.currentTimeMillis() - 1000));

        assertEquals(pool.nextIndex(), -1);
    }

    @Test
    public void nextIndex_whenIssueUntilIsNull_shouldFallBackToExpirationDate() {
        AbstractIndexPool active = pool(0, 9);
        active.setExpirationDate(new Date(System.currentTimeMillis() + HOUR));

        AbstractIndexPool expired = pool(0, 9);
        expired.setExpirationDate(new Date(System.currentTimeMillis() - 1000));

        assertEquals(active.nextIndex(), 0);
        assertEquals(expired.nextIndex(), -1);
    }

    @Test
    public void nextIndex_whenPoolIsExhausted_shouldReturnMinusOne() {
        AbstractIndexPool pool = pool(5, 6);
        pool.setIssueUntil(new Date(System.currentTimeMillis() + HOUR));

        assertEquals(pool.nextIndex(), 5);
        assertEquals(pool.nextIndex(), 6);
        assertEquals(pool.nextIndex(), -1);
    }

    @Test
    public void containsIndex_shouldCheckPoolRange() {
        AbstractIndexPool pool = pool(100, 199);

        assertTrue(pool.containsIndex(100));
        assertTrue(pool.containsIndex(199));
        assertFalse(pool.containsIndex(99));
        assertFalse(pool.containsIndex(200));
    }

    @Test
    public void containsIndex_whenRangeIsNotSet_shouldReturnFalse() {
        assertFalse(new AbstractIndexPool().containsIndex(0));
    }

    @Test
    public void nodeId_shouldBeWrittenDuringUpdate() throws NoSuchFieldException {
        AttributeName attributeName = AbstractIndexPool.class.getDeclaredField("nodeId").getAnnotation(AttributeName.class);

        assertEquals(attributeName.name(), "jansNodeId");
        assertFalse(attributeName.ignoreDuringUpdate());
    }

    private static AbstractIndexPool pool(int startIndex, int endIndex) {
        AbstractIndexPool pool = new AbstractIndexPool();
        pool.setStartIndex(startIndex);
        pool.setEndIndex(endIndex);
        return pool;
    }
}
