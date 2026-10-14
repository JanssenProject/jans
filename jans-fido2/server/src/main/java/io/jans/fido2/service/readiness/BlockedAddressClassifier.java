/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * Says whether a resolved address is one the readiness fetcher must never connect to.
 * <p>
 * Blocked: loopback, private, link-local (which includes the {@code 169.254.169.254} cloud metadata
 * address), unique-local IPv6 (which includes {@code fd00:ec2::254}), multicast, carrier-grade NAT (which
 * includes Alibaba's {@code 100.100.100.200} metadata address), the unspecified and reserved blocks, and
 * IPv6 forms that wrap an IPv4 address (NAT64 and 6to4), judged by the IPv4 address they wrap.
 * <p>
 * An IPv4-mapped IPv6 address ({@code ::ffff:10.0.0.1}) needs no handling here: {@link InetAddress} already
 * hands it back as an IPv4 address. Pure byte comparison; nothing is resolved.
 */
public final class BlockedAddressClassifier {

    private static final List<Range> BLOCKED_V4 = Arrays.asList(
            new Range("0.0.0.0", 8),          // "this network"
            new Range("10.0.0.0", 8),         // private
            new Range("100.64.0.0", 10),      // carrier-grade NAT
            new Range("127.0.0.0", 8),        // loopback
            new Range("169.254.0.0", 16),     // link-local, cloud metadata
            new Range("172.16.0.0", 12),      // private
            new Range("192.0.0.0", 24),       // IETF protocol assignments
            new Range("192.168.0.0", 16),     // private
            new Range("224.0.0.0", 4),        // multicast
            new Range("240.0.0.0", 4));       // reserved, includes the 255.255.255.255 broadcast

    private static final List<Range> BLOCKED_V6 = Arrays.asList(
            new Range("::", 96),              // unspecified, loopback and the deprecated IPv4-compatible form
            new Range("fc00::", 7),           // unique local
            new Range("fe80::", 10),          // link-local
            new Range("fec0::", 10),          // deprecated site-local
            new Range("ff00::", 8),           // multicast
            new Range("64:ff9b:1::", 48));    // local-use NAT64, not routable on the public internet

    private static final Range NAT64 = new Range("64:ff9b::", 96);
    private static final Range SIX_TO_FOUR = new Range("2002::", 16);

    private static final int IPV4_LENGTH = 4;
    private static final int NAT64_EMBEDDED_OFFSET = 12;
    private static final int SIX_TO_FOUR_EMBEDDED_OFFSET = 2;

    private BlockedAddressClassifier() {
    }

    /** True when a connection to this address must be refused. */
    public static boolean isBlocked(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == IPV4_LENGTH) {
            return anyContains(BLOCKED_V4, bytes);
        }
        if (anyContains(BLOCKED_V6, bytes)) {
            return true;
        }
        if (NAT64.contains(bytes)) {
            return isBlockedV4(bytes, NAT64_EMBEDDED_OFFSET);
        }
        if (SIX_TO_FOUR.contains(bytes)) {
            return isBlockedV4(bytes, SIX_TO_FOUR_EMBEDDED_OFFSET);
        }
        return false;
    }

    private static boolean isBlockedV4(byte[] bytes, int offset) {
        return anyContains(BLOCKED_V4, Arrays.copyOfRange(bytes, offset, offset + IPV4_LENGTH));
    }

    private static boolean anyContains(List<Range> ranges, byte[] address) {
        for (Range range : ranges) {
            if (range.contains(address)) {
                return true;
            }
        }
        return false;
    }

    /** A CIDR block. Built only from the literals above, so parsing never performs a DNS lookup. */
    private static final class Range {
        private final byte[] prefix;
        private final int bits;

        Range(String literal, int bits) {
            try {
                this.prefix = InetAddress.getByName(literal).getAddress();
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Invalid built-in range " + literal, e);
            }
            this.bits = bits;
        }

        boolean contains(byte[] address) {
            if (address.length != prefix.length) {
                return false;
            }
            int fullBytes = bits / Byte.SIZE;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != prefix[i]) {
                    return false;
                }
            }
            int remainder = bits % Byte.SIZE;
            if (remainder == 0) {
                return true;
            }
            int mask = (0xFF << (Byte.SIZE - remainder)) & 0xFF;
            return (address[fullBytes] & mask) == (prefix[fullBytes] & mask);
        }
    }
}
