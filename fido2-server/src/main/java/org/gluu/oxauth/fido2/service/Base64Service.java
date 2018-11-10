/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.service;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class Base64Service {

    @Inject
    private Logger log;

    private Encoder base64Encoder;
    private Decoder base64Decoder;

    private Encoder base64UrlEncoder;
    private Decoder base64UrlDecoder;

    @PostConstruct
    public void init() {
        this.base64Encoder = Base64.getEncoder().withoutPadding();
        this.base64Decoder = Base64.getDecoder();

        this.base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
        this.base64UrlDecoder = Base64.getUrlDecoder();
    }

    public String encodeToString(byte[] src) {
        return base64Encoder.encodeToString(src);
    }

    public byte[] decode(byte[] src) {
        return base64Decoder.decode(src);
    }

    public byte[] decode(String src) {
        return base64Decoder.decode(src);
    }

    public String urlEncodeToString(byte[] src) {
        return base64UrlEncoder.encodeToString(src);
    }

    public String urlEncodeToStringWithoutPadding(byte[] src) {
        return base64UrlEncoder.withoutPadding().encodeToString(src);
    }

    public byte[] urlDecode(byte[] src) {
        return base64UrlDecoder.decode(src);
    }

    public byte[] urlDecode(String src) {
        return base64UrlDecoder.decode(src);
    }
}