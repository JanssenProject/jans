/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.io;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Provides an easy way to read a byte array in chunks.
 *
 * @author: Yuriy Movchan Date: 05/20/2015
 */
public class ByteDataInputStream extends DataInputStream {

    public ByteDataInputStream(byte[] data) {
        super(new ByteArrayInputStream(data));
    }

    public byte[] read(int numberOfBytes) throws IOException {
        byte[] readBytes = new byte[numberOfBytes];
        readFully(readBytes);

        return readBytes;
    }

    public byte[] readAll() throws IOException {
        byte[] readBytes = new byte[available()];
        readFully(readBytes);

        return readBytes;
    }

    public byte readSigned() throws IOException {
        return readByte();
    }

    public int readUnsigned() throws IOException {
        return readUnsignedByte();
    }

}
