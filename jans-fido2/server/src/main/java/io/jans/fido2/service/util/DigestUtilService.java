package io.jans.fido2.service.util;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.codec.digest.DigestUtils;

@ApplicationScoped
public class DigestUtilService {

    public byte[] sha256Digest(byte[] input) {
        return DigestUtils.getSha256Digest().digest(input);
    }
}
