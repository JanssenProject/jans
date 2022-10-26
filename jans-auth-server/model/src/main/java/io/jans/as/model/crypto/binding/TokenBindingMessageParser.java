/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.binding;

import com.google.common.base.Preconditions;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.ByteUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingMessageParser {

    private static final Logger log = Logger.getLogger(TokenBindingMessageParser.class);

    private TokenBindingMessageParser() {
    }

    public static List<TokenBinding> parseBase64UrlEncoded(String base64UrlEncodedString) throws TokenBindingParseException {
        return parseBytes(Base64Util.base64urldecode(base64UrlEncodedString));
    }

    public static List<TokenBinding> parseBytes(byte[] raw) throws TokenBindingParseException {
        try {
            int length = ByteUtils.twoBytesAsInt(raw[0], raw[1]);

            if (length != (raw.length - 2)) {
                log.error("Invalid token binding message. First two bytes length value: " + length + "does not match actual bytes length: " + raw.length);
                throw new TokenBindingParseException("Invalid token binding message. First two bytes length value does not match actual bytes length.");
            }

            List<TokenBinding> result = new ArrayList<>();

            TokenBindingStream stream = new TokenBindingStream(raw, 2, raw.length);
            Preconditions.checkState(stream.getPos() == 2);

            while (stream.available() > 0) {
                int tokenTypeAsByteValue = stream.read();

                TokenBindingType tokenBindingType = TokenBindingType.valueOf(tokenTypeAsByteValue);

                int fromID = stream.getPos();
                int keyParametersAsByteValue = stream.read();

                TokenBindingKeyParameters tokenBindingKeyParameters = TokenBindingKeyParameters.valueOf(keyParametersAsByteValue);

                byte[] publicKey = readBytesWithSuffixLength(stream);
                byte[] bindingIdRaw = Arrays.copyOfRange(raw, fromID, stream.getPos());

                byte[] signature = readBytesWithSuffixLength(stream);
                byte[] extensions = readBytesWithSuffixLength(stream);

                TokenBindingID id = new TokenBindingID(tokenBindingKeyParameters, publicKey, bindingIdRaw);

                result.add(new TokenBinding(tokenBindingType, id, signature, new TokenBindingExtension(TokenBindingExtensionType.UNKNOWN, extensions)));
            }
            return result;
        } catch (TokenBindingParseException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenBindingParseException("Failed to parse TokenBindingMessage, raw: " + Base64Util.base64urlencode(raw), e);
        }
    }

    private static byte[] readBytesWithSuffixLength(ByteArrayInputStream stream) {
        int length = ByteUtils.twoIntsAsInt(stream.read(), stream.read());

        byte[] data = new byte[length];
        stream.read(data, 0, length);
        return data;
    }
}
