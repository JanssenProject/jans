package io.jans.fido2.service.util;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.codec.binary.Hex;

@ApplicationScoped
public class HexUtilService {

    public String encodeHexString(byte[] input) {
        return Hex.encodeHexString(input);
    }
}
