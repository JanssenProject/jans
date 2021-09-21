/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.binding;

import com.google.common.base.Function;
import io.jans.as.model.token.JsonWebResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

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

    private List<TokenBinding> tokenBindings;

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
            return jsonWebResponse -> {
                setCnfClaim(jsonWebResponse, referredBinding.getTokenBindingID().sha256base64url(), rpTokenBindingMessageHashClaimKey);
                return null;
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
