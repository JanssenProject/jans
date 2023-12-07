package io.jans.as.server.audit.debug.wrapper;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ServletOutputStreamCopier extends ServletOutputStream {
    private OutputStream outputStream;

    private ByteArrayOutputStream copy;

    public ServletOutputStreamCopier(OutputStream outputStream){
        this.outputStream = outputStream;
        this.copy = new ByteArrayOutputStream(1024);
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
        copy.write(b);
    }

    public byte[] getCopy(){
        return copy.toByteArray();
    }

    @Override
    public boolean isReady() {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new RuntimeException("Not yet implemented");
    }
}
