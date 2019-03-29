/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.io;

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

        char symbol = (char) super.read();
        if (searchFor.startsWith(new String(new char[] {symbol}))) {
            super.mark(searchFor.length());
            StringBuffer startBuffer = new StringBuffer();
            startBuffer.append(symbol);
            for (int i = 1; i < searchFor.length(); i++) {
                startBuffer.append((char) super.read());
            }

            if (searchFor.equals(startBuffer.toString())) {
                this.searchFor = this.end;
                skip = !skip;
                if (!skip) {
                    symbol = (char) read();
                }
                while (skip) {
                    symbol = (char) read();
                }
                this.searchFor = this.start;
                return symbol;
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
            byte symbol = (byte) read();
            if (symbol == -1) {
                return i > 0 ? i : -1;
            }
            b[i] = symbol;
        }
        return i;

    }

}
