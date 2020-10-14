/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.binding;

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
