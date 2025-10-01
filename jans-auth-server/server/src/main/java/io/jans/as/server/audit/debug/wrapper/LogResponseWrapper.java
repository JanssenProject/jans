package io.jans.as.server.audit.debug.wrapper;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class LogResponseWrapper extends ResponseWrapper {

    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private ServletOutputStreamCopier copier;
    private HttpServletResponse res;

    private static final Logger LOG = Logger.getLogger(LogResponseWrapper.class);

    public LogResponseWrapper(HttpServletResponse response) {
        super(response);
        res = response;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called on this response");
        }
        if (outputStream == null) {
            outputStream = getResponse().getOutputStream();
            copier = new ServletOutputStreamCopier(outputStream);
        }
        return copier;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called on this response");
        }
        if (writer == null) {
            copier = new ServletOutputStreamCopier(getResponse().getOutputStream());
            writer = new PrintWriter(new OutputStreamWriter(copier, getResponse().getCharacterEncoding()), true);
        }
        return writer;
    }


    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        } else if (outputStream != null) {
            copier.flush();
        }
    }

    public byte[] getCopy() {
        if (copier != null) {
            return copier.getCopy();
        } else {
            return new byte[0];
        }
    }

    /**
     *
     * @return a string copy of the response body content, if exception returns empty string
     */
    public String getBodyCopy() {
        String responseBody = "";
        try {
           this.flushBuffer();
            byte[] copy = this.getCopy();
            responseBody = new String(copy, res.getCharacterEncoding());
        } catch (IOException ie) {
            LOG.error("Error reading response body content IOException LogResponseWrapper, configuration (httpLoggingResponseBodyContent) is enabled " , ie);
        } finally {
            return responseBody;
        }
    }

}
