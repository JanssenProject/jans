package io.jans.as.server.service.net;

import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
public class PrivateAddressUtilTest {

    @Test
    public void isPrivateAddress_withNat64EmbeddingLinkLocal_shouldReturnTrue() throws Exception {
        assertTrue(PrivateAddressUtil.isPrivateAddress(inet6(0x00, 0x64, 0xff, 0x9b, 0, 0, 0, 0, 0, 0, 0, 0, 169, 254, 169, 254)));
    }

    @Test
    public void isPrivateAddress_withNat64EmbeddingPublicAddress_shouldReturnFalse() throws Exception {
        assertFalse(PrivateAddressUtil.isPrivateAddress(inet6(0x00, 0x64, 0xff, 0x9b, 0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 8, 8)));
    }

    @Test
    public void isPrivateAddress_with6to4EmbeddingPrivateAddress_shouldReturnTrue() throws Exception {
        assertTrue(PrivateAddressUtil.isPrivateAddress(inet6(0x20, 0x02, 10, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Test
    public void isPrivateAddress_with6to4EmbeddingPublicAddress_shouldReturnFalse() throws Exception {
        assertFalse(PrivateAddressUtil.isPrivateAddress(inet6(0x20, 0x02, 8, 8, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Test
    public void isPrivateAddress_withTeredoEmbeddingPrivateAddress_shouldReturnTrue() throws Exception {
        // client IPv4 192.168.1.1 obfuscated by XOR 0xff
        assertTrue(PrivateAddressUtil.isPrivateAddress(inet6(0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x3f, 0x57, 0xfe, 0xfe)));
    }

    @Test
    public void isPrivateAddress_withTeredoEmbeddingPublicAddress_shouldReturnFalse() throws Exception {
        // client IPv4 8.8.8.8 obfuscated by XOR 0xff
        assertFalse(PrivateAddressUtil.isPrivateAddress(inet6(0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xf7, 0xf7, 0xf7, 0xf7)));
    }

    @Test
    public void isPrivateAddress_withIpv4MappedPrivateAddress_shouldReturnTrue() throws Exception {
        assertTrue(PrivateAddressUtil.isPrivateAddress(inet6(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xff, 0xff, 10, 0, 0, 1)));
    }

    @Test
    public void isPrivateAddress_withIpv4MappedPublicAddress_shouldReturnFalse() throws Exception {
        assertFalse(PrivateAddressUtil.isPrivateAddress(inet6(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xff, 0xff, 8, 8, 8, 8)));
    }

    @Test
    public void isPrivateAddress_withIpv4CompatiblePrivateAddress_shouldReturnTrue() throws Exception {
        assertTrue(PrivateAddressUtil.isPrivateAddress(inet6(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 1)));
    }

    @Test
    public void isPrivateAddress_withNat64LocalUsePrefix_shouldReturnTrue() throws Exception {
        assertTrue(PrivateAddressUtil.isPrivateAddress(InetAddress.getByName("64:ff9b:1::1")));
    }

    @Test
    public void isPrivateAddress_withUniqueLocalAddress_shouldReturnTrue() throws Exception {
        assertTrue(PrivateAddressUtil.isPrivateAddress(InetAddress.getByName("fc00::1")));
    }

    @Test
    public void isPrivateAddress_withPublicIpv6Address_shouldReturnFalse() throws Exception {
        assertFalse(PrivateAddressUtil.isPrivateAddress(InetAddress.getByName("2001:4860:4860::8888")));
    }

    @Test
    public void isPrivateAddress_withLoopbackIpv6Address_shouldReturnTrue() throws Exception {
        assertTrue(PrivateAddressUtil.isPrivateAddress(InetAddress.getByName("::1")));
    }

    @Test
    public void reasonForPrivateAddress_withLoopback_shouldNameLoopbackRule() throws Exception {
        assertTrue(PrivateAddressUtil.reasonForPrivateAddress(InetAddress.getByName("127.0.0.1")).contains("loopback"));
    }

    @Test
    public void reasonForPrivateAddress_withNat64EmbeddingLinkLocal_shouldNameNat64AndEmbeddedAddress() throws Exception {
        String reason = PrivateAddressUtil.reasonForPrivateAddress(
                inet6(0x00, 0x64, 0xff, 0x9b, 0, 0, 0, 0, 0, 0, 0, 0, 169, 254, 169, 254));
        assertTrue(reason.contains("NAT64"));
        assertTrue(reason.contains("169.254.169.254"));
        assertTrue(reason.contains("link-local"));
    }

    @Test
    public void reasonForPrivateAddress_with6to4EmbeddingPrivateAddress_shouldName6to4AndEmbeddedAddress() throws Exception {
        String reason = PrivateAddressUtil.reasonForPrivateAddress(inet6(0x20, 0x02, 10, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertTrue(reason.contains("6to4"));
        assertTrue(reason.contains("10.0.0.1"));
    }

    @Test
    public void reasonForPrivateAddress_withNat64LocalUsePrefix_shouldNameRfc8215() throws Exception {
        String reason = PrivateAddressUtil.reasonForPrivateAddress(InetAddress.getByName("64:ff9b:1::1"));
        assertTrue(reason.contains("RFC 8215"));
        assertTrue(reason.contains("64:ff9b:1::/48"));
    }

    @Test
    public void reasonForPrivateAddress_withPublicAddress_shouldReturnNull() throws Exception {
        assertNull(PrivateAddressUtil.reasonForPrivateAddress(InetAddress.getByName("8.8.8.8")));
    }

    private static InetAddress inet6(int... unsignedBytes) throws UnknownHostException {
        byte[] bytes = new byte[unsignedBytes.length];
        for (int i = 0; i < unsignedBytes.length; i++) {
            bytes[i] = (byte) unsignedBytes[i];
        }
        return InetAddress.getByAddress(bytes);
    }
}
