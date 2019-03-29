/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Find pointer to specified count of lines
 *
 * @author Yuriy Movchan Date: 07/12/2013
 */
public class ReverseLineReader {

    private static final int BUFFER_SIZE = 8192;

    private final File file;
    private final String encoding;

    private RandomAccessFile raf;
    private FileChannel channel;
    private long filePos;

    private ByteBuffer buf;
    private long bufPos;

    private ByteArrayOutputStream baos;

    private boolean initialized = false;

    public ReverseLineReader(File file, String encoding) {
        this.file = file;
        this.encoding = encoding;
    }

    private void init() throws IOException {
        synchronized (this) {
            if (this.raf == null) {
                this.raf = new RandomAccessFile(file, "r");
                this.channel = raf.getChannel();
                this.filePos = raf.length();

                this.bufPos = 0;

                this.baos = new ByteArrayOutputStream();

                this.initialized = true;
            }
        }

    }

    public String readLastLine() throws IOException {
        if (!initialized) {
            init();
        }

        while (true) {
            if (bufPos < 0) {
                if (filePos == 0) {
                    if (baos == null) {
                        return null;
                    }

                    String line = buildResultString();
                    baos = null;

                    return line;
                }

                long start = Math.max(filePos - BUFFER_SIZE, 0);
                long end = filePos;
                long len = end - start;

                buf = this.channel.map(FileChannel.MapMode.READ_ONLY, start, len);
                bufPos = len;
            }

            while (bufPos-- > 0) {
                byte c = buf.get((int) bufPos);
                filePos--;
                if (c == '\r' || c == '\n') {
                    if (baos.size() == 0) {
                        continue;
                    } else {
                        return buildResultString();
                    }
                } else {
                    baos.write(c);
                }
            }
        }
    }

    public List<String> readLastLines(int countLines) throws IOException {
        List<String> lastLines = new ArrayList<String>(countLines);
        for (int i = 0; i < countLines; i++) {
            String line = readLastLine();
            if (line == null) {
                break;
            }
            lastLines.add(0, line);
        }

        return lastLines;
    }

    private String buildResultString() throws UnsupportedEncodingException {
        if (baos.size() == 0) {
            return "";
        }

        byte[] bytes = baos.toByteArray();
        swapBytes(bytes);

        baos.reset();
        return new String(bytes, encoding);
    }

    public void swapBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte t = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = t;
        }
    }

    public void close() throws IOException {
        if (this.channel != null) {
            this.channel.close();
        }

        if (this.raf != null) {
            this.raf.close();
        }
    }

}
