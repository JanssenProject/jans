package io.jans.as.server.rate;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author Yuriy Z
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    public static final int MAX_BODY_SIZE = 1024 * 1024; // 1 MB limit

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);

        int contentLength = request.getContentLength();
        if (contentLength > MAX_BODY_SIZE) {
            throw new IOException("Request body exceeds maximum allowed size: " + MAX_BODY_SIZE);
        }

        // Read the entire body and cache it.
        InputStream is = request.getInputStream();
        this.cachedBody = is.readNBytes(MAX_BODY_SIZE + 1);

        if (this.cachedBody.length > MAX_BODY_SIZE) {
            throw new IOException("Actual body size exceeded 1MB limit.");
        }
    }

    public byte[] getCachedBody() {
        return cachedBody;
    }

    public String getCachedBodyAsString() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);

        return new ServletInputStream() {
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setReadListener(ReadListener readListener) { }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
