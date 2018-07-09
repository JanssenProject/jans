package org.xdi.oxauth.model.util;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ByteUtils {

    private ByteUtils() {
    }

    public static int twoBytesAsInt(byte one, byte two) {
        return (byteAsInt(one) << 8) | byteAsInt(two);
    }

    public static int twoIntsAsInt(int one, int two) {
        return (one << 8) | two;
    }

    public static int byteAsInt(byte value) {
        return value & 0xff;
    }
}
