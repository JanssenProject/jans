package io.jans.ca.rs.protect;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/12/2015
 */

public class RsProtectorTest {

    @Test
    public void access() throws IOException {
        final RsProtector protector = RsProtector.instance(fileInputStream("simple.json"));

        assertTrue(protector.hasAccess("/photo", "GET", "http://photoz.example.com/dev/actions/view"));
        assertTrue(protector.hasAccess("/photo", "PUT", "http://photoz.example.com/dev/actions/add"));
        assertTrue(protector.hasAccess("/photo", "POST", "http://photoz.example.com/dev/actions/add",
                "http://photoz.example.com/dev/actions/all"));

        assertFalse(protector.hasAccess("/photo", "GET", "http://photoz.example.com/dev/actions/add"));
        assertFalse(protector.hasAccess("/photo", "PUT", "http://photoz.example.com/dev/actions/view"));
    }

    private InputStream fileInputStream(String fileName) {
        return RsProtectorTest.class.getResourceAsStream(fileName);
    }
}
