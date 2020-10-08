package org.gluu.oxauth.model.crypto.binding;

import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxauth.model.token.JsonWebResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * struct {
 *     TokenBinding tokenbindings<132..2^16-1>;
 * } TokenBindingMessage;
 * </pre>
 *
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingMessage {

    private static final Logger log = Logger.getLogger(TokenBindingMessage.class);

    private List<TokenBinding> tokenBindings = new ArrayList<TokenBinding>();

    public TokenBindingMessage(String base64urlencoded) throws TokenBindingParseException {
        this(TokenBindingMessageParser.parseBase64UrlEncoded(base64urlencoded));
    }

    public TokenBindingMessage(byte[] raw) throws TokenBindingParseException {
        this(TokenBindingMessageParser.parseBytes(raw));
    }

    public TokenBindingMessage(List<TokenBinding> tokenBindings) {
        this.tokenBindings = tokenBindings;
    }

    public List<TokenBinding> getTokenBindings() {
        return tokenBindings;
    }

    public TokenBinding getFirstTokenBindingByType(TokenBindingType type) {
        for (TokenBinding binding : tokenBindings) {
            if (binding.getTokenBindingType() == type) {
                return binding;
            }
        }
        return null;
    }

    public static Function<JsonWebResponse, Void> createIdTokenTokingBindingPreprocessing(String tokenBindingMessageAsString, final String rpTokenBindingMessageHashClaimKey) throws TokenBindingParseException {
        final boolean tokenBindingMessagePresent = StringUtils.isNotBlank(tokenBindingMessageAsString);
        final boolean rpKeyPresent = StringUtils.isNotBlank(rpTokenBindingMessageHashClaimKey);

        log.trace("TokenBindingMessage present: " + tokenBindingMessagePresent + ", rpCnfKey: " + rpTokenBindingMessageHashClaimKey);

        if (tokenBindingMessagePresent && rpKeyPresent) {
            TokenBindingMessage message = new TokenBindingMessage(tokenBindingMessageAsString);
            final TokenBinding referredBinding = message.getFirstTokenBindingByType(TokenBindingType.REFERRED_TOKEN_BINDING);
            return new Function<JsonWebResponse, Void>() {
                @Override
                public Void apply(JsonWebResponse jsonWebResponse) {
                    setCnfClaim(jsonWebResponse, referredBinding.getTokenBindingID().sha256base64url(), rpTokenBindingMessageHashClaimKey);
                    return null;
                }
            };
        }
        return null;
    }

    public static void setCnfClaim(JsonWebResponse jsonWebResponse, String tokenBindingIdHash, String rpTokenBindingMessageHashClaimKey) {
        try {
            JSONObject value = jsonWebResponse.getClaims().getClaimAsJSON("cnf");
            if (value == null) {
                value = new JSONObject();
            }
            value.put(rpTokenBindingMessageHashClaimKey, tokenBindingIdHash);

            jsonWebResponse.getClaims().setClaim("cnf", value);
        } catch (JSONException e) {
            log.error("Failed to create cnf JSON object", e);
        }
    }

    public static String getTokenBindingIdHashFromTokenBindingMessage(String tokenBindingMessageAsString, final String rpTokenBindingMessageHashClaimKey) throws TokenBindingParseException {
        if (StringUtils.isNotBlank(tokenBindingMessageAsString) && StringUtils.isNotBlank(rpTokenBindingMessageHashClaimKey)) {
            TokenBindingMessage message = new TokenBindingMessage(tokenBindingMessageAsString);
            final TokenBinding referredBinding = message.getFirstTokenBindingByType(TokenBindingType.REFERRED_TOKEN_BINDING);
            return referredBinding.getTokenBindingID().sha256base64url();
        }
        return null;
    }

    @Override
    public String toString() {
        return "TokenBindingMessage{" +
                "tokenBindings=" + tokenBindings +
                '}';
    }
}
