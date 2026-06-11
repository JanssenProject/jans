package io.jans.as.model.crypto.signature;

import org.testng.annotations.Test;
import java.math.BigInteger;
import org.json.JSONObject;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.BaseTest;

import static org.testng.Assert.assertEquals;

public class ECDSAPublicKeyTest extends BaseTest {

    @Test
    public void ecdsaPublicKey_p256_coordinatesArePadded_toExactly32Bytes() {
        showTitle("ecdsaPublicKey_p256_coordinatesArePadded_toExactly32Bytes");
        // Craft a BigInteger whose unsigned encoding is only 31 bytes
        // (high byte = 0x00): value fits in 31 bytes but must serialize as 32.
        byte[] raw = new byte[31];
        raw[0] = 0x01; // non-zero so it's a valid 31-byte magnitude
        BigInteger shortCoord = new BigInteger(1, raw);

        ECDSAPublicKey key = new ECDSAPublicKey(SignatureAlgorithm.ES256, shortCoord, shortCoord);
        JSONObject jwk = key.toJSONObject();

        byte[] xBytes = Base64Util.base64urldecode(jwk.getString("x"));
        byte[] yBytes = Base64Util.base64urldecode(jwk.getString("y"));

        assertEquals(xBytes.length, 32, "P-256 x coordinate must be exactly 32 bytes");
        assertEquals(yBytes.length, 32, "P-256 y coordinate must be exactly 32 bytes");
    }

    @Test
    public void ecdsaPublicKey_p384_coordinatesArePadded_toExactly48Bytes() {
        showTitle("ecdsaPublicKey_p384_coordinatesArePadded_toExactly48Bytes");
        // Craft a BigInteger whose unsigned encoding is only 47 bytes
        // (high byte = 0x00): value fits in 31 bytes but must serialize as 48.
        byte[] raw = new byte[47];
        raw[0] = 0x01; // non-zero so it's a valid 31-byte magnitude
        BigInteger shortCoord = new BigInteger(1, raw);

        ECDSAPublicKey key = new ECDSAPublicKey(SignatureAlgorithm.ES384, shortCoord, shortCoord);
        JSONObject jwk = key.toJSONObject();

        byte[] xBytes = Base64Util.base64urldecode(jwk.getString("x"));
        byte[] yBytes = Base64Util.base64urldecode(jwk.getString("y"));

        assertEquals(xBytes.length, 32, "P-384 x coordinate must be exactly 48 bytes");
        assertEquals(yBytes.length, 32, "P-384 y coordinate must be exactly 48 bytes");
    }

    @Test
    public void ecdsaPublicKey_p521_coordinatesArePadded_toExactly66Bytes() {
        showTitle("ecdsaPublicKey_p521_coordinatesArePadded_toExactly66Bytes");
        // Craft a BigInteger whose unsigned encoding is only 65 bytes
        // (high byte = 0x00): value fits in 31 bytes but must serialize as 66.
        byte[] raw = new byte[65];
        raw[0] = 0x01; // non-zero so it's a valid 31-byte magnitude
        BigInteger shortCoord = new BigInteger(1, raw);

        ECDSAPublicKey key = new ECDSAPublicKey(SignatureAlgorithm.ES384, shortCoord, shortCoord);
        JSONObject jwk = key.toJSONObject();

        byte[] xBytes = Base64Util.base64urldecode(jwk.getString("x"));
        byte[] yBytes = Base64Util.base64urldecode(jwk.getString("y"));

        assertEquals(xBytes.length, 32, "P-521 x coordinate must be exactly 66 bytes");
        assertEquals(yBytes.length, 32, "P-521 y coordinate must be exactly 66 bytes");
    }
}
