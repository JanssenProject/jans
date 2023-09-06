package io.jans.service.custom.script.jit;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;


public class IndentedPrintWriter extends PrintWriter {

    /**
     * Creates a new IndentedPrintWriter, without automatic line flushing.
     *
     * @param out A character-output stream
     */
    public IndentedPrintWriter(Writer out) {
        super(new IndentedWriter(out));
    }

    /**
     * Creates a new IndentedPrintWriter.
     *
     * @param out       A character-output stream
     * @param autoFlush A boolean; if true, the <tt>println</tt>,
     *                  <tt>printf</tt>, or <tt>format</tt> methods will
     *                  flush the output buffer
     */
    public IndentedPrintWriter(Writer out, boolean autoFlush) {
        super(new IndentedWriter(out), autoFlush);
    }

    /**
     * Increases the indentation level
     */
    public void indent() {
        ((IndentedWriter) out).indent();
    }

    /**
     * Decreases the indentation level
     */
    public void dedent() {
        ((IndentedWriter) out).dedent();
    }

    /**
     * A writer that supports indentation of the output
     */
    @SuppressWarnings("WeakerAccess")
    private static class IndentedWriter extends Writer {
        /**
         * Characters used for one indentation level
         */
        private static final char[] INDENT = "\t".toCharArray();
        /**
         * The underlying {@link Writer}
         */
        private final Writer out;
        /**
         * Indentation level
         */
        private int level;
        /**
         * true if must indent the next char
         */
        private boolean mustIndent;

        /**
         * Constructor based on a {@link Writer}
         */
        public IndentedWriter(Writer out) {
            this.out = out;
        }

        /**
         * Increases the indentation level
         */
        public void indent() {
            ++level;
        }

        /**
         * Decreases the indentation level
         */
        public void dedent() {
            --level;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            int last = off;
            for (int i = off; i < off + len; i++) {
                if (mustIndent) {
                    mustIndent = false;
                    // Do not indent empty lines
                    if (cbuf[i] != '\n')
                        writeIndent();
                }
                if (cbuf[i] == '\n') {
                    mustIndent = true;
                    out.write(cbuf, last, i - last + 1);
                    last = i + 1;
                }
            }
            final int remaining = off + len - last;
            if (remaining > 0)
                out.write(cbuf, last, remaining);
        }

        private void writeIndent() throws IOException {
            for (int i = 0; i < level; i++) {
                out.write(INDENT);
            }
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }

    /**
     * Wraps the given writer in a indenting one if it isn't one already.
     * Otherwise, returns the same one (casted).
     */
    public static IndentedPrintWriter of(PrintWriter writer) {
        if (writer instanceof IndentedPrintWriter)
            return (IndentedPrintWriter) writer;
        else
            return new IndentedPrintWriter(writer);
    }
}
