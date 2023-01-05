package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PairTest extends BaseTest {

    @Test
    public void Pair_testConstructorGetterSetter_correctvalues() {
        showTitle("Pair_testConstructorGetterSetter_correctvalues");
        String fisrt = "first";
        String second = "second";
        Pair<String,String> pair = new Pair<>(fisrt, second);
        assertEquals(pair.getFirst(), fisrt);
        assertEquals(pair.getSecond(), second);
        pair.setFirst("1");
        pair.setSecond("2");
        assertNotEquals(pair.getFirst(), fisrt);
        assertNotEquals(pair.getSecond(), second);
    }

    @Test
    public void equals_pairsToCompare_trueAndFalse() {
        showTitle("equals_pairsToCompare_trueAndFalse");
        String fisrt = "first";
        String second = "second";
        String third = "third";
        Pair<String,String> pair = new Pair<>(fisrt, second);
        Pair<String,String> pair2 = new Pair<>(fisrt, second);
        Pair<String,String> pair3 = new Pair<>(fisrt, third);
        assertTrue(pair.equals(pair2));
        assertFalse(pair.equals(pair3));
    }

    @Test
    public void hashCode_validPair_validInthashCode() {
        showTitle("hashCode_validPair_validInthashCode");
        String fisrt = "first";
        String second = "second";
        Pair<String,String> pair = new Pair<>(fisrt, second);
        assertEquals(pair.hashCode(), 2114373572);
    }

    @Test
    public void toString_validPair_validString() {
        showTitle("toString_validPair_validString");
        String fisrt = "first";
        String second = "second";
        Pair<String,String> pair = new Pair<>(fisrt, second);
        assertEquals(pair.toString(), "(first, second)");
    }

}
