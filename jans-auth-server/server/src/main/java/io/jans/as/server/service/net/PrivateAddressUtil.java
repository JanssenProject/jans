/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.service.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Shared SSRF-protection check for client-supplied URLs (sector_identifier_uri, client_id URL).
 * In addition to the standard {@link InetAddress} private/loopback/link-local/multicast checks and
 * the IPv6 Unique Local Address range (fc00::/7, not covered by {@link InetAddress#isSiteLocalAddress()}),
 * this also unwraps IPv6 transition mechanisms that embed an IPv4 address (NAT64, 6to4, Teredo,
 * IPv4-mapped and IPv4-compatible addresses) and re-checks the embedded address, since those can be
 * used to reach private IPv4 destinations under an address that otherwise looks globally routable.
 *
 * @author Yuriy Z
 */
public class PrivateAddressUtil {

    private static final byte[] NAT64_WELL_KNOWN_PREFIX = {0x00, 0x64, (byte) 0xff, (byte) 0x9b};
    private static final byte SIXTOFOUR_PREFIX_HIGH = 0x20;
    private static final byte SIXTOFOUR_PREFIX_LOW = 0x02;
    private static final byte TEREDO_PREFIX_HIGH = 0x20;
    private static final byte TEREDO_PREFIX_LOW_1 = 0x01;

    private PrivateAddressUtil() {
    }

    public static boolean isPrivateAddress(InetAddress address) {
        return reasonForPrivateAddress(address) != null;
    }

    /**
     * Same check as {@link #isPrivateAddress(InetAddress)}, but returns a human-readable reason
     * identifying exactly which rule matched (and, for IPv6 transition mechanisms, which embedded
     * IPv4 address and rule it resolved to), for use in rejection log messages. Returns null if the
     * address is public.
     */
    public static String reasonForPrivateAddress(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return "loopback address (InetAddress.isLoopbackAddress)";
        }
        if (address.isSiteLocalAddress()) {
            return "site-local/RFC1918 private address (InetAddress.isSiteLocalAddress)";
        }
        if (address.isLinkLocalAddress()) {
            return "link-local address, e.g. cloud metadata range 169.254.0.0/16 (InetAddress.isLinkLocalAddress)";
        }
        if (address.isAnyLocalAddress()) {
            return "wildcard/any-local address (InetAddress.isAnyLocalAddress)";
        }
        if (address.isMulticastAddress()) {
            return "multicast address (InetAddress.isMulticastAddress)";
        }

        final byte[] bytes = address.getAddress();
        if (bytes.length != 16) {
            return null;
        }
        if ((bytes[0] & 0xFE) == 0xFC) {
            return "IPv6 Unique Local Address in fc00::/7 (PrivateAddressUtil ULA check)";
        }

        final EmbeddedIpv4 embedded = extractEmbeddedIpv4(bytes);
        if (embedded == null) {
            return null;
        }
        final String embeddedReason = reasonForPrivateAddress(embedded.address);
        if (embeddedReason == null) {
            return null;
        }
        return embedded.mechanism + " address embedding IPv4 " + embedded.address.getHostAddress()
                + ", which is a " + embeddedReason;
    }

    private static final class EmbeddedIpv4 {
        final String mechanism;
        final InetAddress address;

        EmbeddedIpv4(String mechanism, InetAddress address) {
            this.mechanism = mechanism;
            this.address = address;
        }
    }

    /**
     * Extracts an embedded IPv4 address from known IPv6 transition mechanisms, or returns null if
     * none of them match. Recognizes: NAT64 (64:ff9b::/96), 6to4 (2002::/16), Teredo (2001:0::/32),
     * IPv4-mapped (::ffff:a.b.c.d) and IPv4-compatible (::a.b.c.d, deprecated).
     */
    private static EmbeddedIpv4 extractEmbeddedIpv4(byte[] bytes) {
        try {
            if (matches(bytes, 0, NAT64_WELL_KNOWN_PREFIX) && allZero(bytes, 4, 12)) {
                return new EmbeddedIpv4("NAT64 (64:ff9b::/96)", InetAddress.getByAddress(Arrays.copyOfRange(bytes, 12, 16)));
            }
            if (bytes[0] == SIXTOFOUR_PREFIX_HIGH && bytes[1] == SIXTOFOUR_PREFIX_LOW) {
                return new EmbeddedIpv4("6to4 (2002::/16)", InetAddress.getByAddress(Arrays.copyOfRange(bytes, 2, 6)));
            }
            if (bytes[0] == TEREDO_PREFIX_HIGH && bytes[1] == TEREDO_PREFIX_LOW_1 && bytes[2] == 0x00 && bytes[3] == 0x00) {
                // Teredo obfuscates the embedded client IPv4 address by XOR-ing it with 0xff
                final byte[] ipv4 = new byte[4];
                for (int i = 0; i < 4; i++) {
                    ipv4[i] = (byte) (bytes[12 + i] ^ 0xFF);
                }
                return new EmbeddedIpv4("Teredo (2001::/32)", InetAddress.getByAddress(ipv4));
            }
            if (allZero(bytes, 0, 10) && (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF) {
                return new EmbeddedIpv4("IPv4-mapped (::ffff:a.b.c.d)", InetAddress.getByAddress(Arrays.copyOfRange(bytes, 12, 16)));
            }
            if (allZero(bytes, 0, 12) && !allZero(bytes, 12, 16)) {
                return new EmbeddedIpv4("IPv4-compatible (::a.b.c.d)", InetAddress.getByAddress(Arrays.copyOfRange(bytes, 12, 16)));
            }
        } catch (UnknownHostException e) {
            // 4-byte address, always resolvable without I/O; not reachable in practice
            return null;
        }
        return null;
    }

    private static boolean matches(byte[] bytes, int offset, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean allZero(byte[] bytes, int from, int to) {
        for (int i = from; i < to; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
