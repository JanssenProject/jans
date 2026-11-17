package io.jans.configapi.plugin.facade.convert;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

final class ProtoWire {

    private static final int VARINT = 0;
    private static final int FIXED32 = 5;
    private static final int LENGTH_DELIMITED = 2;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    ProtoWire string(int field, String value) {
        return bytes(field, value.getBytes(StandardCharsets.UTF_8));
    }

    ProtoWire bytes(int field, byte[] value) {
        if (value.length == 0) {
            return this;
        }
        tag(field, LENGTH_DELIMITED);
        varint(value.length);
        out.writeBytes(value);
        return this;
    }

    ProtoWire message(int field, ProtoWire message) {
        byte[] bytes = message.toByteArray();
        tag(field, LENGTH_DELIMITED);
        varint(bytes.length);
        out.writeBytes(bytes);
        return this;
    }

    ProtoWire int64(int field, long value) {
        if (value == 0) {
            return this;
        }
        tag(field, VARINT);
        varint(value);
        return this;
    }

    ProtoWire enumValue(int field, int value) {
        return int64(field, value);
    }

    ProtoWire floatValue(int field, float value) {
        if (value == 0f) {
            return this;
        }
        tag(field, FIXED32);
        out.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array());
        return this;
    }

    byte[] toByteArray() {
        return out.toByteArray();
    }

    private void tag(int field, int wireType) {
        varint(((long) field << 3) | wireType);
    }

    private void varint(long value) {
        long v = value;
        while ((v & ~0x7FL) != 0) {
            out.write((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        out.write((int) v);
    }
}
