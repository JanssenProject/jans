package org.gluu.persist.key.impl;

import org.testng.annotations.Test;

import static org.gluu.persist.key.impl.KeyShortcuter.fromShortcut;
import static org.gluu.persist.key.impl.KeyShortcuter.shortcut;
import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class KeyShortcuterTest {

    @Test
    public void prefixDropped() {
        assertEquals(shortcut("gluuAttributeName"), "attrName");
        assertEquals(shortcut("gluuAttributeType"), "attr_t");
        assertEquals(shortcut("oxAuthAppType"), "app_t");
        assertEquals(shortcut("oxAuthLogoutSessionRequired"), "logoutSessionRequired");
        assertEquals(shortcut("oxIconUrl"), "iconUrl");
        assertEquals(shortcut("oxTrustActive"), "active");

        // reverse
        assertEquals(fromShortcut("attrName"), "gluuAttributeName");
        assertEquals(fromShortcut("attr_t"), "gluuAttributeType");
        assertEquals(fromShortcut("app_t"), "oxAuthAppType");
        assertEquals(fromShortcut("logoutSessionRequired"), "oxAuthLogoutSessionRequired");
        assertEquals(fromShortcut("iconUrl"), "oxIconUrl");
        assertEquals(fromShortcut("active"), "oxTrustActive");
    }

    @Test
    public void shortcutsWithMarkers() {
        assertEquals(shortcut("gluuGroupVisibility"), "_gVisibility");
        assertEquals(shortcut("oxAuthTrustedClient"), "trusted_c");
        assertEquals(shortcut("oxAuthSubjectType"), "subject_t");
        assertEquals(shortcut("oxAuthUserId"), "_uId");
        assertEquals(shortcut("oxAuthUserDN"), "_uDN");
        assertEquals(shortcut("oxAuthDefaultAcrValues"), "_dAcrValues");
        assertEquals(shortcut("uniqueIdentifier"), "_id");
        assertEquals(shortcut("oxId"), "id");

        // reverse
        assertEquals(fromShortcut("_gVisibility"), "gluuGroupVisibility");
        assertEquals(fromShortcut("trusted_c"), "oxAuthTrustedClient");
        assertEquals(fromShortcut("subject_t"), "oxAuthSubjectType");
        assertEquals(fromShortcut("_uId"), "oxAuthUserId");
        assertEquals(fromShortcut("_uDN"), "oxAuthUserDN");
        assertEquals(fromShortcut("_dAcrValues"), "oxAuthDefaultAcrValues");
        assertEquals(fromShortcut("_id"), "uniqueIdentifier");
        assertEquals(fromShortcut("id"), "oxId");
    }

    @Test
    public void shortcutsWithoutMarkers() {
        assertEquals(shortcut("oxSmtpConfiguration"), "smtpConf");
        assertEquals(shortcut("oxTrustConfApplication"), "_trConfApp");
        assertEquals(shortcut("oxTrustConfApplication"), "_trConfApp"); // same again by intention
        assertEquals(shortcut("oxAuthUserInfoEncryptedResponseAlg"), "_uInfoEncRespAlg");
        assertEquals(shortcut("oxAuthAuthenticationTime"), "authnTime");
        assertEquals(shortcut("oxIDPAuthentication"), "iDPAuthn");
        assertEquals(shortcut("oxAuthSkipAuthorization"), "skipAuthz");
        assertEquals(shortcut("oxAuthTokenEndpointAuthSigningAlg"), "tokEndpointAuthSigAlg");
        assertEquals(shortcut("oxLinkExpirationDate"), "linkExpDate");
        assertEquals(shortcut("oxAuthRequestObjectEncryptionAlg"), "reqObjEncAlg");
        assertEquals(shortcut("oxAuthTokenType"), "tok_t");
        assertEquals(shortcut("oxAuthTokenEndpointAuthSigningAlg"), "tokEndpointAuthSigAlg");
        assertEquals(shortcut("oxDeletable"), "del");
        assertEquals(shortcut("description"), "desc");

        // reverse
        assertEquals(fromShortcut("smtpConf"), "oxSmtpConfiguration");
        assertEquals(fromShortcut("_trConfApp"), "oxTrustConfApplication");
        assertEquals(fromShortcut("_trConfApp"), "oxTrustConfApplication");// same again by intention
        assertEquals(fromShortcut("_uInfoEncRespAlg"), "oxAuthUserInfoEncryptedResponseAlg");
        assertEquals(fromShortcut("authnTime"), "oxAuthAuthenticationTime");
        assertEquals(fromShortcut("iDPAuthn"), "oxIDPAuthentication");
        assertEquals(fromShortcut("skipAuthz"), "oxAuthSkipAuthorization");
        assertEquals(fromShortcut("tokEndpointAuthSigAlg"), "oxAuthTokenEndpointAuthSigningAlg");
        assertEquals(fromShortcut("linkExpDate"), "oxLinkExpirationDate");
        assertEquals(fromShortcut("reqObjEncAlg"), "oxAuthRequestObjectEncryptionAlg");
        assertEquals(fromShortcut("tok_t"), "oxAuthTokenType");
        assertEquals(fromShortcut("tokEndpointAuthSigAlg"), "oxAuthTokenEndpointAuthSigningAlg");
        assertEquals(fromShortcut("del"), "oxDeletable");
        assertEquals(fromShortcut("desc"), "description");
    }
}
