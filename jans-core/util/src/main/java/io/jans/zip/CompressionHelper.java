/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.io.IOUtils;

/**
 * Allow to defltate/infalte byte array
 *
 * @author Yuriy Movchan Date: 04/24/2014
 */
public final class CompressionHelper {

    public static final int DEFAULT_MAX_INFLATED_SIZE = 16 * 1024 * 1024;

    private CompressionHelper() { }

    public static byte[] deflate(byte[] data, boolean nowrap) throws IOException {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, nowrap);
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                os.write(buffer, 0, count);
            }
        } finally {
            IOUtils.closeQuietly(os);
        }

        return os.toByteArray();
    }

    public static byte[] inflate(byte[] data, boolean nowrap) throws IOException, DataFormatException {
        return inflate(data, nowrap, DEFAULT_MAX_INFLATED_SIZE);
    }

    public static byte[] inflate(byte[] data, boolean nowrap, int maxInflatedSize) throws IOException, DataFormatException {
        Inflater inflater = new Inflater(nowrap);
        inflater.setInput(data);

        ByteArrayOutputStream os = new ByteArrayOutputStream(Math.min(data.length, maxInflatedSize));
        try {
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if ((count == 0) && !inflater.finished()) {
                    throw new DataFormatException("Incomplete compressed data");
                }
                if (os.size() + count > maxInflatedSize) {
                    throw new DataFormatException("Inflated data exceeds limit of " + maxInflatedSize + " bytes");
                }
                os.write(buffer, 0, count);
            }
        } finally {
            inflater.end();
            IOUtils.closeQuietly(os);
        }

        return os.toByteArray();
    }

}
