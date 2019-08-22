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

    @Test
    public void shortcutsWithMarkers() {
        assertEquals(shortcut("gluuGroupVisibility"), "_gVisibility");
        assertEquals(shortcut("oxAuthTrustedClient"), "trusted_c");
        assertEquals(shortcut("oxAuthSubjectType"), "subject_t");
        assertEquals(shortcut("oxAuthUserId"), "_uId");
        assertEquals(shortcut("oxAuthUserDN"), "_uDN");
        assertEquals(shortcut("oxAuthDefaultAcrValues"), "_dAcrValues");
    }

    @Test
    public void shortcutsWithoutMarkers() {
        assertEquals(shortcut("oxSmtpConfiguration"), "smtpConf");
        assertEquals(shortcut("oxTrustConfApplication"), "confApp");
        assertEquals(shortcut("oxAuthUserInfoEncryptedResponseAlg"), "_uInfoEncRespAlg");
        assertEquals(shortcut("oxAuthAuthenticationTime"), "authnTime");
        assertEquals(shortcut("oxIDPAuthentication"), "iDPAuthn");
        assertEquals(shortcut("oxAuthSkipAuthorization"), "skipAuthz");
        assertEquals(shortcut("oxAuthTokenEndpointAuthSigningAlg"), "tokEndpointAuthSigAlg");
        assertEquals(shortcut("oxLinkExpirationDate"), "linkExpDate");
        assertEquals(shortcut("oxAuthRequestObjectEncryptionAlg"), "reqObjEncAlg");
        assertEquals(shortcut("oxAuthTokenType"), "tok_t");
        assertEquals(shortcut("oxAuthTokenEndpointAuthSigningAlg"), "tokEndpointAuthSigAlg");
    }
}
