/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.zip;

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
        Inflater inflater = new Inflater(nowrap);
        inflater.setInput(data);

        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        try {
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                os.write(buffer, 0, count);
            }
        } finally {
            IOUtils.closeQuietly(os);
        }

        return os.toByteArray();
    }

}
