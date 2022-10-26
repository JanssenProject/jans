package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;

public class Base64UtilTest extends BaseTest {

    private static final String TEXT_EXAMPLE = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
    private static final byte[] BYTE_ARRAY_EXAMPLE = TEXT_EXAMPLE.getBytes(StandardCharsets.UTF_8);
    private static final String TEXT_BASE64_EXAMPLE = "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlzIHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2YgdGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGludWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRoZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=";
    private static final String TEXT_BASE64_URL_EXAMPLE = "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlzIHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2YgdGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGludWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRoZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4";
    private static final String TEXT_HEX_EXAMPLE = "4d616e2069732064697374696e677569736865642c206e6f74206f6e6c792062792068697320726561736f6e2c2062757420627920746869732073696e67756c61722070617373696f6e2066726f6d206f7468657220616e696d616c732c2077686963682069732061206c757374206f6620746865206d696e642c20746861742062792061207065727365766572616e6365206f662064656c6967687420696e2074686520636f6e74696e75656420616e6420696e6465666174696761626c652067656e65726174696f6e206f66206b6e6f776c656467652c2065786365656473207468652073686f727420766568656d656e6365206f6620616e79206361726e616c20706c6561737572652e";

    private static final BigInteger BIG_INTEGER_UNSIGNED_EXAMPLE = BigInteger.valueOf(123);
    private static final int[] PLAIN_TEXT_INT_ARRAY_UNSIGNED = {65, 66, 67, 68};
    private static final byte[] PLAIN_TEXT_BYTE_ARRAY_UNSIGNED = {65, 66, 67, 68};
    private static final String BIG_INTEGER_TEXT_URL_BASE64 = "ew";

    @Test
    public void base64urlencode_validByteArray_correctStringBase64Encoded() {
        showTitle("base64urlencode_validByteArray_correctStringBase64Encoded");
        String result = Base64Util.base64urlencode(BYTE_ARRAY_EXAMPLE);
        assertEquals(result, TEXT_BASE64_URL_EXAMPLE, "Base64Util.base64urlencode(byte[]) have returned an incorrect base64 String");
    }

    @Test
    public void base64urldecode_validUrlStringBase64Encoded_correctBytArray() {
        showTitle("base64urldecode_validUrlStringBase64Encoded_correctBytArray");
        byte[] byteArray = Base64Util.base64urldecode(TEXT_BASE64_URL_EXAMPLE);
        assertEquals(byteArray, BYTE_ARRAY_EXAMPLE, "Base64Util.base64urldecode(string) have returned an incorrect byte[] array");
    }

    @Test
    public void base64urldecodeToString_validUrlStringBase64Encoded_correctStringPlainText() {
        showTitle("base64urldecodeToString_validUrlStringBase64Encoded_correctStringPlainText");
        String stringResult = Base64Util.base64urldecodeToString(TEXT_BASE64_URL_EXAMPLE);
        assertEquals(stringResult, TEXT_EXAMPLE, "Base64Util.base64urldecodeToString(string) have returned an incorrect String ");
    }

    @Test
    public void base64urlencodeUnsignedBigInt_validBigInteger_stringBase64Encoded() {
        showTitle("base64urlencodeUnsignedBigInt_validBigInteger_stringBase64Encoded");
        String stringResult = Base64Util.base64urlencodeUnsignedBigInt(BIG_INTEGER_UNSIGNED_EXAMPLE);
        assertEquals(stringResult, BIG_INTEGER_TEXT_URL_BASE64, "Base64Util.base64urlencodeUnsignedBigInt(BigInteger) have returned an incorrect String ");
    }

    @Test
    public void bytesToHex_validByteArrayPlainText_correctStringHex() {
        showTitle("bytesToHex_validByteArrayPlainText_correctStringHex");
        String stringResult = Base64Util.bytesToHex(BYTE_ARRAY_EXAMPLE);
        assertEquals(stringResult, TEXT_HEX_EXAMPLE, "Base64Util.bytesToHex(byte[]) have returned an incorrect Hex String ");
    }

    @Test
    public void unsignedToBytes_validintArrayPlainText_correctByteArray() {
        showTitle("unsignedToBytes_validintArrayPlainText_correctByteArray");
        byte[] byteArray = Base64Util.unsignedToBytes(PLAIN_TEXT_INT_ARRAY_UNSIGNED);
        assertEquals(byteArray, PLAIN_TEXT_BYTE_ARRAY_UNSIGNED, "Base64Util.unsignedToBytes(int[]) have returned an incorrect Byte[] ");
    }

}
