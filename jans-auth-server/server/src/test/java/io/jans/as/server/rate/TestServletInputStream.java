package io.jans.as.server.rate;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class TestServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream bodyInputStream;

    public TestServletInputStream(String body) {
        this(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    public TestServletInputStream(ByteArrayInputStream bodyInputStream) {
        this.bodyInputStream = bodyInputStream;
    }

    @Override
    public int read() {
        return bodyInputStream.read();
    }

    @Override
    public boolean isFinished() {
        return bodyInputStream.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
    }

}
