/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BlockedAddressClassifierTest {

    private static boolean blocked(String literal) throws UnknownHostException {
        // Every argument is an IP literal, so this never performs a DNS lookup.
        return BlockedAddressClassifier.isBlocked(InetAddress.getByName(literal));
    }

    /** Columns: first and last address inside each blocked IPv4 range. The addresses just outside are in the allow test. */
    @ParameterizedTest
    @CsvSource({
            "0.0.0.0, 0.255.255.255",
            "10.0.0.0, 10.255.255.255",
            "100.64.0.0, 100.127.255.255",
            "127.0.0.0, 127.255.255.255",
            "169.254.0.0, 169.254.255.255",
            "172.16.0.0, 172.31.255.255",
            "192.0.0.0, 192.0.0.255",
            "192.0.2.0, 192.0.2.255",
            "192.168.0.0, 192.168.255.255",
            "198.18.0.0, 198.19.255.255",
            "198.51.100.0, 198.51.100.255",
            "203.0.113.0, 203.0.113.255",
            "224.0.0.0, 239.255.255.255",
            "240.0.0.0, 255.255.255.255",
    })
    void blocksIpv4Ranges_atTheirEdges(String first, String last) throws UnknownHostException {
        assertTrue(blocked(first), first);
        assertTrue(blocked(last), last);
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.0.0.0", "9.255.255.255", "11.0.0.0", "100.63.255.255", "100.128.0.0",
            "126.255.255.255", "128.0.0.0", "169.253.255.255", "169.255.0.0", "172.15.255.255", "172.32.0.0",
            "191.255.255.255", "192.0.1.0", "192.167.255.255", "192.169.0.0", "223.255.255.255", "8.8.8.8",
            "192.0.1.255", "192.0.3.0", "198.17.255.255", "198.20.0.0", "198.51.99.255", "198.51.101.0",
            "203.0.112.255", "203.0.114.0",
            "93.184.216.34" })
    void allowsPublicIpv4_includingTheAddressesJustOutsideEachRange(String literal) throws UnknownHostException {
        assertFalse(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "169.254.169.254", "100.100.100.200", "255.255.255.255", "127.0.0.1", "10.0.0.1",
            "192.168.1.1", "172.16.0.1", "224.0.0.251" })
    void blocksTheWellKnownDangerousAddresses(String literal) throws UnknownHostException {
        assertTrue(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "::", "::1", "::10.0.0.1", "fe80::1", "febf:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
            "fc00::", "fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "fd00:ec2::254", "fec0::1", "ff02::1", "ff00::",
            "64:ff9b:1::1" })
    void blocksIpv6Ranges(String literal) throws UnknownHostException {
        assertTrue(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "2606:4700:4700::1111", "2001:4860:4860::8888", "fbff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
            "fe00::1" })
    void allowsPublicIpv6_andTheAddressesJustOutsideEachRange(String literal) throws UnknownHostException {
        assertFalse(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "::ffff:127.0.0.1", "::ffff:10.0.0.1", "::ffff:169.254.169.254", "::ffff:192.168.0.1",
            "::ffff:100.100.100.200" })
    void blocksIpv4MappedIpv6_ofABlockedIpv4Address(String literal) throws UnknownHostException {
        assertTrue(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "::ffff:8.8.8.8", "::ffff:93.184.216.34" })
    void allowsIpv4MappedIpv6_ofAPublicIpv4Address(String literal) throws UnknownHostException {
        assertFalse(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "64:ff9b::7f00:1", "64:ff9b::a00:1", "64:ff9b::a9fe:a9fe", "64:ff9b::c0a8:1" })
    void blocksNat64_wrappingABlockedIpv4Address(String literal) throws UnknownHostException {
        assertTrue(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "64:ff9b::808:808" })
    void allowsNat64_wrappingAPublicIpv4Address(String literal) throws UnknownHostException {
        assertFalse(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "2002:7f00:1::", "2002:a00:1::", "2002:a9fe:a9fe::", "2002:c0a8:1::" })
    void blocksSixToFour_wrappingABlockedIpv4Address(String literal) throws UnknownHostException {
        assertTrue(blocked(literal), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = { "2002:808:808::", "2003::1" })
    void allowsSixToFour_wrappingAPublicIpv4Address_andTheNeighbouringPrefix(String literal)
            throws UnknownHostException {
        assertFalse(blocked(literal), literal);
    }
}
