/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.net.IDN;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a relying party ID may be used to build an outbound URL.
 * <p>
 * Pure string validation: it never resolves DNS. It accepts a DNS hostname with at least two labels and
 * nothing else, which is what rules out the SSRF-shaped inputs: IP literals (including the decimal and hex
 * forms such as {@code 2130706433} and {@code 0x7f.1}), {@code localhost}, internal-only suffixes,
 * credentials, ports, paths, queries and fragments.
 * <p>
 * <b>IDN rule.</b> An internationalized name is converted with {@link IDN#toASCII(String)} and the result
 * is what is returned. A label whose letters come from more than one script (for example Latin mixed with
 * Cyrillic) is rejected, whether it was written as Unicode or as {@code xn--} punycode: the punycode form
 * is decoded and checked too, so it cannot be used to smuggle a homograph through. This is deliberately
 * conservative; a legitimate name that mixes scripts, such as Japanese Han with Kana, is rejected too.
 */
public final class RpIdValidator {

    /** Why a value was rejected. */
    public enum Rejection {
        EMPTY, CONTROL_OR_WHITESPACE, CREDENTIALS, PORT, PATH_QUERY_OR_FRAGMENT, IP_LITERAL, RESERVED_NAME,
        MIXED_SCRIPT, TOO_LONG, MALFORMED
    }

    /** Outcome of {@link #validate(String)}: either an accepted ID or a rejection reason. */
    public static final class Result {
        private final ValidatedRpId rpId;
        private final Rejection rejection;

        private Result(ValidatedRpId rpId, Rejection rejection) {
            this.rpId = rpId;
            this.rejection = rejection;
        }

        public boolean isValid() {
            return rpId != null;
        }

        /** The accepted ID, or null when the value was rejected. */
        public ValidatedRpId rpId() {
            return rpId;
        }

        /** The rejection reason, or null when the value was accepted. */
        public Rejection rejection() {
            return rejection;
        }
    }

    private static final int MAX_HOSTNAME_LENGTH = 253;
    private static final int MAX_LABEL_LENGTH = 63;
    private static final String[] RESERVED_SUFFIXES = { ".localhost", ".local", ".internal", ".corp", ".home.arpa" };

    private RpIdValidator() {
    }

    public static Result validate(String value) {
        if (value == null || value.isEmpty()) {
            return reject(Rejection.EMPTY);
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= ' ' || c == 0x7f || Character.isWhitespace(c) || Character.isISOControl(c)) {
                return reject(Rejection.CONTROL_OR_WHITESPACE);
            }
        }
        if (value.indexOf('@') >= 0) {
            return reject(Rejection.CREDENTIALS);
        }
        if (value.indexOf('[') >= 0 || value.indexOf(']') >= 0 || isIpv6Shaped(value)) {
            return reject(Rejection.IP_LITERAL);
        }
        if (value.indexOf(':') >= 0) {
            return reject(Rejection.PORT);
        }
        if (value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || value.indexOf('?') >= 0
                || value.indexOf('#') >= 0) {
            return reject(Rejection.PATH_QUERY_OR_FRAGMENT);
        }

        String ascii;
        try {
            ascii = IDN.toASCII(value).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return reject(Rejection.MALFORMED);
        }
        if (ascii.endsWith(".")) {
            ascii = ascii.substring(0, ascii.length() - 1);
        }
        if (ascii.isEmpty()) {
            return reject(Rejection.EMPTY);
        }
        if (ascii.length() > MAX_HOSTNAME_LENGTH) {
            return reject(Rejection.TOO_LONG);
        }

        String[] labels = ascii.split("\\.", -1);
        if (isIpv4Shaped(labels)) {
            return reject(Rejection.IP_LITERAL);
        }
        if (isReserved(ascii)) {
            return reject(Rejection.RESERVED_NAME);
        }
        if (labels.length < 2) {
            return reject(Rejection.MALFORMED);
        }
        for (String label : labels) {
            if (label.length() > MAX_LABEL_LENGTH) {
                return reject(Rejection.TOO_LONG);
            }
            if (!label.matches("[a-z0-9]([a-z0-9-]*[a-z0-9])?")) {
                return reject(Rejection.MALFORMED);
            }
            if (mixesScripts(label)) {
                return reject(Rejection.MIXED_SCRIPT);
            }
        }
        return new Result(new ValidatedRpId(ascii), null);
    }

    private static Result reject(Rejection rejection) {
        return new Result(null, rejection);
    }

    /** Colon-bearing text made only of hex digits, colons and dots: an IPv6 literal, with or without a mapped IPv4 tail. */
    private static boolean isIpv6Shaped(String value) {
        return value.indexOf(':') >= 0 && value.matches("[0-9a-fA-F:.]+");
    }

    /**
     * A name whose last label is numeric is read as an IPv4 address by resolvers ({@code 2130706433},
     * {@code 127.1}, {@code 0x7f.1}). No real top-level domain is numeric, so rejecting it costs nothing.
     */
    private static boolean isIpv4Shaped(String[] labels) {
        String last = labels[labels.length - 1];
        return last.matches("[0-9]+") || last.matches("0x[0-9a-f]*");
    }

    private static boolean isReserved(String hostname) {
        if (hostname.equals("localhost")) {
            return true;
        }
        for (String suffix : RESERVED_SUFFIXES) {
            if (hostname.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the letters of a label (decoded from punycode if need be) come from more than one script. */
    private static boolean mixesScripts(String asciiLabel) {
        String decoded = IDN.toUnicode(asciiLabel);
        Set<Character.UnicodeScript> scripts = new HashSet<>();
        decoded.codePoints().filter(Character::isLetter).forEach(cp -> {
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED) {
                scripts.add(script);
            }
        });
        return scripts.size() > 1;
    }
}
