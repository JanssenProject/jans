package org.xdi.oxauth.model.crypto.binding;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.ByteUtils;

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

            List<TokenBinding> result = new ArrayList<TokenBinding>();

            TokenBindingStream stream = new TokenBindingStream(raw, 2, raw.length);
            Preconditions.checkState(stream.getPos() == 2);

            while (stream.available() > 0) {
                int tokenTypeAsByteValue = stream.read();

                TokenBindingType tokenBindingType = TokenBindingType.valueOf(tokenTypeAsByteValue);
                if (tokenBindingType == null) {
                    throw new TokenBindingParseException("Failed to identify TokenBindingType, byteValue: " + tokenTypeAsByteValue);
                }

                int fromID = stream.getPos();
                int keyParametersAsByteValue = stream.read();

                TokenBindingKeyParameters tokenBindingKeyParameters = TokenBindingKeyParameters.valueOf(keyParametersAsByteValue);
                if (tokenBindingKeyParameters == null) {
                    throw new TokenBindingParseException("Failed to identify TokenBindingKeyParameters, byteValue: " + keyParametersAsByteValue);
                }

                byte[] publicKey = readBytesWithSuffixLength(stream);
                byte[] bindingIdRaw = Arrays.copyOfRange(raw, fromID, stream.getPos());

                byte[] signature = readBytesWithSuffixLength(stream);
                byte[] extensions = readBytesWithSuffixLength(stream);

                TokenBindingID id = new TokenBindingID(tokenBindingKeyParameters, publicKey, bindingIdRaw);

                result.add(new TokenBinding(tokenBindingType, id, signature, new TokenBindingExtension(TokenBindingExtensionType.UNKNOWN, extensions)));
            }
            return result;
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
