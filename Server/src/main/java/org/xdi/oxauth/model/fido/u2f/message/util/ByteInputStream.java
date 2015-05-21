/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.fido.u2f.message.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Provides an easy way to read a byte array in chunks.
 */
//  ByteArrayInputStream cannot throw IOExceptions, so this class is converting checked exceptions to unchecked.
public class ByteInputStream extends DataInputStream {

    public ByteInputStream(byte[] data) {
        super(new ByteArrayInputStream(data));
    }

    public byte[] read(int numberOfBytes) {
        byte[] readBytes = new byte[numberOfBytes];
        try {
            readFully(readBytes);
        } catch (IOException e) {
            throw new AssertionError();
        }
        return readBytes;
    }

    public byte[] readAll() {
        try {
            byte[] readBytes = new byte[available()];
            readFully(readBytes);
            return readBytes;
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    public int readInteger() {
        try {
            return readInt();
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    public byte readSigned() {
        try {
            return readByte();
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    public int readUnsigned() {
        try {
            return readUnsignedByte();
        } catch (IOException e) {
            throw new AssertionError();
        }
    }
}
