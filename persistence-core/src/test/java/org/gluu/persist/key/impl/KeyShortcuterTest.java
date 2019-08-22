package org.gluu.persist.key.impl;

import org.testng.annotations.Test;

import static org.gluu.persist.key.impl.KeyShortcuter.shortcut;
import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class KeyShortcuterTest {

    @Test
    public void prefixDropped() {
        assertEquals(shortcut("gluuAttributeType"), "attr_t");
        assertEquals(shortcut("oxAuthAppType"), "app_t");
        assertEquals(shortcut("oxAuthLogoutSessionRequired"), "logoutSessionRequired");
        assertEquals(shortcut("oxIconUrl"), "iconUrl");
        assertEquals(shortcut("oxTrustActive"), "active");
    }
}
