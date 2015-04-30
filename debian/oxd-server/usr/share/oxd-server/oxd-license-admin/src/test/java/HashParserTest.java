import junit.framework.Assert;
import org.testng.annotations.Test;
import org.xdi.oxd.license.admin.client.HashParser;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/11/2014
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
