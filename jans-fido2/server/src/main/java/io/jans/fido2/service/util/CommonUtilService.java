package io.jans.fido2.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

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

    public static JsonNode toJsonNode(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(Objects.requireNonNullElse(obj, "{}"));
    }
}
