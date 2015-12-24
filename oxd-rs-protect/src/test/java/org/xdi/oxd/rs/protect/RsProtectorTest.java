package org.xdi.oxd.rs.protect;

import junit.framework.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/12/2015
 */

public class RsProtectorTest {

    @Test
    public void access() throws IOException {
        final RsProtector protector = RsProtector.instance(fileInputStream("simple.json"));
        Assert.assertTrue(protector.hasAccess("/photo", "http://photoz.example.com/dev/actions/print"));
        Assert.assertTrue(protector.hasAccess("/photo",  "http://photoz.example.com/dev/actions/print",
                "http://photoz.example.com/dev/actions/add"));

        Assert.assertFalse(protector.hasAccess("/photo", "http://photoz.example.com/dev/actions/view"));
        Assert.assertFalse(protector.hasAccess("/photo", "http://photoz.example.com/dev/actions/print",
                "http://photoz.example.com/dev/actions/view"));
    }

    private InputStream fileInputStream(String fileName) {
        return RsProtectorTest.class.getResourceAsStream(fileName);
    }
}
