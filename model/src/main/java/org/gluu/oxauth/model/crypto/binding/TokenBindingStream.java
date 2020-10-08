package org.gluu.oxauth.model.crypto.binding;

import java.io.ByteArrayInputStream;

/**
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingStream extends ByteArrayInputStream {
    public TokenBindingStream(byte[] buf) {
        super(buf);
    }

    public TokenBindingStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    public int getPos() {
        return pos;
    }
}
