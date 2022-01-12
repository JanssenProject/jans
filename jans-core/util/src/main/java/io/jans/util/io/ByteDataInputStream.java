/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.io;

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
