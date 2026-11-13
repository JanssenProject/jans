/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExcludeFilterInputStream extends BufferedInputStream {

    private String end = "";
    private String start = "";
    private String searchFor = "";
    private boolean skip = false;

    public ExcludeFilterInputStream(InputStream in, String start, String end) {
        super(in);
        this.start = start;
        this.end = end;
        this.searchFor = this.start;
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        if (c == -1) {
            return -1;
        }

        char symbol = (char) c;
        if (searchFor.startsWith(new String(new char[] {symbol}))) {
            super.mark(searchFor.length());
            StringBuffer startBuffer = new StringBuffer();
            startBuffer.append(symbol);
            for (int i = 1; i < searchFor.length(); i++) {
                int next = super.read();
                if (next == -1) {
                    break;
                }
                startBuffer.append((char) next);
            }

            if (searchFor.equals(startBuffer.toString())) {
                this.searchFor = this.end;
                skip = !skip;
                int result = -1;
                if (!skip) {
                    result = read();
                }
                while (skip) {
                    result = read();
                    if (result == -1) {
                        // Unterminated excluded region
                        skip = false;
                    }
                }
                this.searchFor = this.start;
                return result;
            } else {
                super.reset();
                return (symbol);
            }
        }
        return symbol;

    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i;
        for (i = 0; i < len; i++) {
            int symbol = read();
            if (symbol == -1) {
                return i > 0 ? i : -1;
            }
            b[off + i] = (byte) symbol;
        }
        return i;

    }

}
