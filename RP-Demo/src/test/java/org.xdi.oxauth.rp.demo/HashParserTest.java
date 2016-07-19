package org.xdi.oxauth.rp.demo;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author yuriyz on 07/19/2016.
 */
public class HashParserTest {

    @Test
    public  void test() {
        assertHash("#id_token=tt", "tt");
        assertHash("#nonce=nonce&id_token=tt1", "tt1");
        assertHash("#nonce=nonce&id_token=tt2&param=param1", "tt2");
    }

    private static void assertHash(String hash, String expectedIdToken) {
        final String actual = HashParser.getIdTokenFromHash(hash);
        System.out.println("Actual: " + actual + ", Expected:" + expectedIdToken);
        Assert.assertEquals(actual, expectedIdToken);
    }

}
