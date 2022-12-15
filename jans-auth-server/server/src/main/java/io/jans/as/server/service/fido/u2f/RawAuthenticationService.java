/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.fido.u2f;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.apache.commons.codec.binary.Hex;
import io.jans.as.model.exception.SignatureException;
import io.jans.as.model.fido.u2f.exception.BadInputException;
import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.crypto.signature.SHA256withECDSASignatureVerification;
import io.jans.util.io.ByteDataInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;

/**
 * Provides operations with U2F RAW authentication response
 *
 * @author Yuriy Movchan Date: 05/20/2015
 */
@ApplicationScoped
@Named
public class RawAuthenticationService {

    public static final String AUTHENTICATE_GET_TYPE = "navigator.id.getAssertion";
    public static final String AUTHENTICATE_CANCEL_TYPE = "navigator.id.cancelAssertion";
    public static final String[] SUPPORTED_AUTHENTICATE_TYPES = new String[]{AUTHENTICATE_GET_TYPE, AUTHENTICATE_CANCEL_TYPE};

    @Inject
    private Logger log;

    @Inject 
    //@Named(value = "sha256withECDSASignatureVerification")
    private SHA256withECDSASignatureVerification signatureVerification;

    public RawAuthenticateResponse parseRawAuthenticateResponse(String rawDataBase64) {
        ByteDataInputStream bis = new ByteDataInputStream(Base64Util.base64urldecode(rawDataBase64));
        try {
            return new RawAuthenticateResponse(bis.readSigned(), bis.readInt(), bis.readAll());
        } catch (IOException ex) {
            throw new BadInputException("Failed to parse RAW authenticate response", ex);
        } finally {
            IOUtils.closeQuietly(bis);
        }
    }

    public void checkSignature(String appId, ClientData clientData, RawAuthenticateResponse rawAuthenticateResponse, byte[] publicKey) throws BadInputException {
        String rawClientData = clientData.getRawClientData();

        byte[] signedBytes = packBytesToSign(signatureVerification.hash(appId), rawAuthenticateResponse.getUserPresence(),
                rawAuthenticateResponse.getCounter(), signatureVerification.hash(rawClientData));

        log.debug("Packed bytes to sign in HEX '{}'", Hex.encodeHexString(signedBytes));
        log.debug("Signature from authentication response in HEX '{}'", Hex.encodeHexString(rawAuthenticateResponse.getSignature()));
        try {
            boolean isValid = signatureVerification.checkSignature(signatureVerification.decodePublicKey(publicKey),
                    signedBytes, rawAuthenticateResponse.getSignature());
            if (!isValid) {
                throw new BadInputException("Signature is not valid");
            }
        } catch (SignatureException ex) {
            throw new BadInputException("Failed to checkSignature", ex);
        }
    }

    private byte[] packBytesToSign(byte[] appIdHash, byte userPresence, long counter, byte[] challengeHash) {
        ByteArrayDataOutput encoded = ByteStreams.newDataOutput();
        encoded.write(appIdHash);
        encoded.write(userPresence);
        encoded.writeInt((int) counter);
        encoded.write(challengeHash);

        return encoded.toByteArray();
    }

}
