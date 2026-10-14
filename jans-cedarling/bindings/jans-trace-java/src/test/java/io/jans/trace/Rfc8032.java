// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

/** RFC 8032 section 7.1 TEST 1 key; RFC 8037 appendix A uses the same key. Test-only. */
final class Rfc8032 {

    static final byte[] SECRET = hex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
    static final byte[] PUBLIC = hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a");
    static final String X = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo";
    static final String JWK = "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"" + X + "\"}";

    /** RFC 8037 A.3 thumbprint {@code kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k} as a TRACE digest. */
    static final String THUMBPRINT =
            "sha256:90facafea9b1556698540f70c0117a22ea37bd5cf3ed3c47093c1707282b4b89";

    /** The secret as unencrypted PKCS#8 PEM, as {@code openssl genpkey -algorithm ed25519} writes it. */
    static final String PEM = "-----BEGIN PRIVATE KEY-----\n"
            + "MC4CAQAwBQYDK2VwBCIEIJ1hsZ3v/VpguoRK9JLsLMREScVpezJpGXA7rAMcrn9g\n"
            + "-----END PRIVATE KEY-----\n";

    /** Signature of the empty message under the key, unpadded base64url. */
    static final String SIGNATURE =
            "5VZDAMNgrHKQhuLMgG6CioSHfx645dl02HPgZSJJAVVfuIIVkKM7rMYeOXAc-bRr0lv18FlbviRlUUFDjnoQCw";

    private Rfc8032() {
    }

    static byte[] hex(String text) {
        byte[] bytes = new byte[text.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(text.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
