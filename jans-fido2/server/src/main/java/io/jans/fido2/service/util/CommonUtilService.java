package io.jans.fido2.service.util;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class CommonUtilService {

    public ByteArrayOutputStream writeOutputStreamByteList(List<byte[]> list) throws IOException {
        if (list.isEmpty()) {
            throw new IOException("List is empty");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte[] bytes : list) {
            baos.write(bytes);
        }
        return baos;
    }
}
