/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

@Named
public class AuthenticatorDataParser {

    @Inject
    private Logger log;

    @Inject
    @Named("base64UrlDecoder")
    private Base64.Decoder base64UrlDecoder;

    @Inject
    @Named("base64Decoder")
    private Base64.Decoder base64Decoder;

    @Inject
    private CommonVerifiers commonVerifiers;

    AuthData parseAttestationData(String incomingAuthData) {
        return parseAuthData(incomingAuthData, true);
    }

    public AuthData parseAssertionData(String incomingAuthData) {
        return parseAuthData(incomingAuthData, false);
    }

    private AuthData parseAuthData(String incomingAuthData, boolean isAttestation) {
        AuthData authData = new AuthData();
        byte[] buffer;

        if (isAttestation)
            buffer = base64Decoder.decode(incomingAuthData.getBytes());
        else {
            buffer = base64UrlDecoder.decode(incomingAuthData.getBytes());
        }
        authData.setAuthDataDecoded(buffer);
        int offset = 0;
        byte[] rpIdHashBuffer = Arrays.copyOfRange(buffer, offset, offset += 32);
        log.info("RPIDHASH hex {}", Hex.encodeHexString(rpIdHashBuffer));
        byte[] flagsBuffer = Arrays.copyOfRange(buffer, offset, offset += 1);

        boolean hasAtFlag = commonVerifiers.verifyAtFlag(flagsBuffer);
        log.info("FLAGS hex {}", Hex.encodeHexString(flagsBuffer));

        byte[] counterBuffer = Arrays.copyOfRange(buffer, offset, offset += 4);
        log.info("COUNTERS hex {}", Hex.encodeHexString(counterBuffer));
        authData.setRpIdHash(rpIdHashBuffer).setFlags(flagsBuffer).setCounters(counterBuffer);
        byte[] attestationBuffer = Arrays.copyOfRange(buffer, offset, buffer.length);
        commonVerifiers.verifyAttestationBuffer(hasAtFlag, attestationBuffer);

        if (hasAtFlag) {
            byte[] aaguidBuffer = Arrays.copyOfRange(buffer, offset, offset += 16);
            log.info("AAGUID hex {}", Hex.encodeHexString(aaguidBuffer));

            byte[] credIDLenBuffer = Arrays.copyOfRange(buffer, offset, offset += 2);
            log.info("CredIDLen hex {}", Hex.encodeHexString(credIDLenBuffer));
            int size = ByteBuffer.wrap(credIDLenBuffer).asShortBuffer().get();
            log.info("size {}", size);
            byte[] credIDBuffer = Arrays.copyOfRange(buffer, offset, offset += size);
            log.info("credID hex {}", Hex.encodeHexString(credIDBuffer));

            byte[] cosePublicKeyBuffer = Arrays.copyOfRange(buffer, offset, buffer.length);
            log.info("cosePublicKey hex {}", Hex.encodeHexString(cosePublicKeyBuffer));
            CBORFactory f = new CBORFactory();

            long keySize = 0;
            CBORParser parser = null;
            try {
                parser = f.createParser(cosePublicKeyBuffer);
                while (!parser.isClosed()) {
                    JsonToken t = parser.nextToken();
                    JsonLocation tocloc = parser.getTokenLocation();
                    if (t.isStructEnd()) {
                        keySize = tocloc.getByteOffset();
                        break;
                    }
                }
            } catch (IOException e) {
                throw new Fido2RPRuntimeException(e.getMessage());
            } finally {
                if (parser != null) {
                    try {
                        parser.close();
                    } catch (IOException e) {
                        log.info("exception when closing a parser {}", e.getMessage());
                    }
                }
            }
            offset += keySize;
            ObjectMapper mapper = new ObjectMapper(f);

            int keyType = -100;
            try {
                JsonNode key = mapper.readTree(cosePublicKeyBuffer);
                keyType = key.get("3").asInt();
                log.info("cosePublicKey {}", key);
            } catch (IOException e) {
                throw new Fido2RPRuntimeException("Unable to parse public key CBOR");
            }
            authData.setAaguid(aaguidBuffer).setCredId(credIDBuffer).setCOSEPublicKey(cosePublicKeyBuffer).setKeyType(keyType);
            byte[] leftovers = Arrays.copyOfRange(buffer, offset, buffer.length);
            commonVerifiers.verifyNoLeftovers(leftovers);
        }
        authData.setAttestationBuffer(buffer);

        return authData;
    }

    public int parseCounter(byte[] counter) {
        int cnt = ByteBuffer.wrap(counter).asIntBuffer().get();
        return cnt;
    }
}
