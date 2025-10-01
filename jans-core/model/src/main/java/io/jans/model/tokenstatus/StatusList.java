package io.jans.model.tokenstatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.InflaterInputStream;

/**
 * @author Yuriy Z
 */
public class StatusList {

    private BitSet list;
    private final int bits;
    private final int divisor;

    public StatusList(int bits) {
        this.divisor = 8 / bits;
        this.list = new BitSet(bits);
        this.bits = bits;
    }

    public static StatusList fromEncoded(String encoded, int bits) throws IOException {
        StatusList newList = new StatusList(bits);
        newList.decode(encoded);
        return newList;
    }

    public String encodeAsString() throws IOException {
        byte[] byteArray = this.list.toByteArray();
        byte[] zipped = compress(byteArray);
//        System.out.println("encode zipped: " + new String(zipped));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(zipped);
    }

    public byte[] encodeAsBytes() throws IOException {
        byte[] byteArray = this.list.toByteArray();
        return compress(byteArray);
    }

    public Map<String, Object> encodeAsJSON() throws IOException {
        String encodedList = encodeAsString();
        Map<String, Object> object = new HashMap<>();
        object.put("bits", this.bits);
        object.put("lst", encodedList);
        return object;
    }

    public void decode(String input) throws IOException {
        byte[] zipped = Base64.getUrlDecoder().decode(input + "=".repeat((4 - input.length() % 4) % 4));
//        System.out.println("decode - zipped: " + new String(zipped));
        this.list = BitSet.valueOf(decompress(zipped));
    }

    public void set(int pos, int value) {
//        int rest = pos % this.divisor;
//        int floored = pos / this.divisor;
//        int shift = rest * this.bits;
//        int mask = 0xFF ^ (((1 << this.bits) - 1) << shift);
//        this.list[floored] = (byte) ((this.list[floored] & mask) + (value << shift));
        for (int i = 0; i < this.bits; i++) {
            boolean bitValue = ((value >> i) & 1) == 1;
            this.list.set(pos * this.bits + i, bitValue);
        }
    }

    public void validateSetValue(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Status value can't be less then 0.");
        }

        switch (bits) {
            case 1:
                if (value > 1) {
                    throw new IllegalArgumentException("Value can't be more then 1. With bits " + bits + ".");
                }
                return;
            case 2:
                if (value > 3) {
                    throw new IllegalArgumentException("Value can't be more then 3. With bits " + bits + ".");
                }
                return;
            case 4:
                if (value > 15) {
                    throw new IllegalArgumentException("Value can't be more then 15. With bits " + bits + ".");
                }
                return;
            case 8:
                if (value > 255) {
                    throw new IllegalArgumentException("Value can't be more then 255. With bits " + bits + ".");
                }
                return;
        }

        throw new IllegalArgumentException(String.format("Status value can't be %s. With bits %s.", value, bits));
    }

    public int get(int pos) {
//        int rest = pos % this.divisor;
//        int floored = pos / this.divisor;
//        int shift = rest * this.bits;
//        return (this.list[floored] & 0xff & (((1 << this.bits) - 1) << shift)) >> shift;
        int value = 0;
        for (int i = 0; i < this.bits; i++) {
            if (this.list.get(pos * this.bits + i)) {
                value |= (1 << i);
            }
        }
        return value;
    }

    public int getBitSetLength() {
        return list.length();
    }

    @Override
    public String toString() {
        StringBuilder val = new StringBuilder();
        int size = list.length() / 8 * this.divisor;
        for (int x = 0; x < size; x++) {
            val.append(Integer.toHexString(this.get(x)));
        }
        return val.toString();
    }

    private static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }

    private static byte[] decompress(byte[] data) throws IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inflaterInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    public int getBits() {
        return bits;
    }

    public int getDivisor() {
        return divisor;
    }

    public String getLst() throws IOException {
        return encodeAsString();
    }
}
