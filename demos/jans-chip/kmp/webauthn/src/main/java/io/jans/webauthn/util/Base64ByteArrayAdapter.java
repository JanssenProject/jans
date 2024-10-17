package io.jans.webauthn.util;

import android.util.Base64;
import android.util.Log;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Base64ByteArrayAdapter is a TypeAdapter for byte arrays that automatically encodes/decodes them
 * as Base64-encoded strings.
 * <p>
 * This makes it easier to communicate with Golang-based APIs that use the native JSON marshalling
 * functionality, which automatically treats byte array types as Base64-encoed strings.
 */
public class Base64ByteArrayAdapter extends TypeAdapter<byte[]> {
    @Override
    public void write(JsonWriter out, byte[] value) {
        try {
            if (value == null) {
                out.nullValue();
                return;
            }

            String base64Encoded = Base64.encodeToString(value, Base64.NO_WRAP);
            out.value(base64Encoded);
        } catch (IOException e) {
            Log.d("Base64ByteArrayAdapter", e.toString());
        }

    }

    @Override
    public byte[] read(JsonReader in) {
        try {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            String base64Encoded = in.nextString();
            return Base64.decode(base64Encoded, Base64.NO_WRAP);
        } catch (IOException e) {
            Log.d("Base64ByteArrayAdapter", e.toString());
            return null;
        }
    }
}
