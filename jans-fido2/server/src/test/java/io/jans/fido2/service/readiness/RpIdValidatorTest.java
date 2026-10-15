/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.IDN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.jans.fido2.service.readiness.RpIdValidator.Rejection;
import io.jans.fido2.service.readiness.RpIdValidator.Result;

class RpIdValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "example.com, example.com",
            "Login.Example.CO.UK, login.example.co.uk",
            "example.com., example.com",
            "a-b.example.com, a-b.example.com",
            "xn--bcher-kva.example, xn--bcher-kva.example",
    })
    void accepts_dnsHostnames_andNormalizesThem(String input, String expected) {
        Result result = RpIdValidator.validate(input);

        assertTrue(result.isValid(), input);
        assertEquals(expected, result.rpId().hostname());
        assertNull(result.rejection());
    }

    @Test
    void accepts_singleScriptIdn_andReturnsPunycode() {
        // German umlaut name, written as Unicode.
        Result result = RpIdValidator.validate("bücher.example");

        assertTrue(result.isValid());
        assertEquals("xn--bcher-kva.example", result.rpId().hostname());
    }

    @Test
    void accepts_singleScriptNonLatinName() {
        // Entirely Cyrillic: one script, so not a homograph.
        assertTrue(RpIdValidator.validate("пример.example").isValid());
    }

    @ParameterizedTest
    @CsvSource({
            "127.0.0.1, IP_LITERAL",
            "2130706433, IP_LITERAL",
            "0x7f.1, IP_LITERAL",
            "127.1, IP_LITERAL",
            "[::1], IP_LITERAL",
            "::1, IP_LITERAL",
            "::ffff:127.0.0.1, IP_LITERAL",
            "fe80::1, IP_LITERAL",
            "localhost, RESERVED_NAME",
            "api.localhost, RESERVED_NAME",
            "a.local, RESERVED_NAME",
            "a.internal, RESERVED_NAME",
            "a.corp, RESERVED_NAME",
            "router.home.arpa, RESERVED_NAME",
            "user@example.com, CREDENTIALS",
            "user:pw@example.com, CREDENTIALS",
            "example.com:8443, PORT",
            "example.com/path, PATH_QUERY_OR_FRAGMENT",
            "example.com?x=1, PATH_QUERY_OR_FRAGMENT",
            "example.com#f, PATH_QUERY_OR_FRAGMENT",
            "example, MALFORMED",
            "-bad.example.com, MALFORMED",
            "bad-.example.com, MALFORMED",
            "a..example.com, MALFORMED",
            "exa_mple.com, MALFORMED",
    })
    void rejects_ssrfShapedInput(String input, Rejection expected) {
        Result result = RpIdValidator.validate(input);

        assertFalse(result.isValid(), input);
        assertEquals(expected, result.rejection());
        assertNull(result.rpId());
    }

    @Test
    void rejects_backslashPath() {
        assertEquals(Rejection.PATH_QUERY_OR_FRAGMENT, RpIdValidator.validate("example.com\\path").rejection());
    }

    @Test
    void rejects_emptyAndNull() {
        assertEquals(Rejection.EMPTY, RpIdValidator.validate(null).rejection());
        assertEquals(Rejection.EMPTY, RpIdValidator.validate("").rejection());
        assertEquals(Rejection.EMPTY, RpIdValidator.validate(".").rejection());
    }

    @ParameterizedTest
    @ValueSource(strings = { " example.com", "example.com ", "exa mple.com", "example.com\n", "example.com\t",
            "example.com\u0000", "example.com\u007f" })
    void rejects_whitespaceAndControlCharacters(String input) {
        assertEquals(Rejection.CONTROL_OR_WHITESPACE, RpIdValidator.validate(input).rejection());
    }

    @Test
    void rejects_overLongLabelAndOverLongName() {
        String longLabel = "a".repeat(64) + ".example.com";
        String longName = ("a".repeat(60) + ".").repeat(5) + "com";

        assertFalse(RpIdValidator.validate(longLabel).isValid());
        assertEquals(Rejection.TOO_LONG, RpIdValidator.validate(longName).rejection());
    }

    @Test
    void rejects_mixedScriptHomograph_writtenAsUnicode() {
        // "p?ypal.com" where the second letter is Cyrillic U+0430.
        assertEquals(Rejection.MIXED_SCRIPT, RpIdValidator.validate("pаypal.com").rejection());
    }

    @Test
    void rejects_mixedScriptHomograph_writtenAsPunycode() {
        String punycode = IDN.toASCII("pаypal.com");
        assertTrue(punycode.startsWith("xn--"), punycode);

        assertEquals(Rejection.MIXED_SCRIPT, RpIdValidator.validate(punycode).rejection());
    }

    @Test
    void rejects_fullwidthLookalikesOfSeparators() {
        // NFKC folds the fullwidth colon and slash to ':' and '/', which must still be refused.
        assertFalse(RpIdValidator.validate("example.com：8443").isValid());
        assertFalse(RpIdValidator.validate("example.com／path").isValid());
    }
}
