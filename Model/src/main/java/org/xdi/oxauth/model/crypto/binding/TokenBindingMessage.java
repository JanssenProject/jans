package org.xdi.oxauth.model.crypto.binding;

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

    @Override
    public String toString() {
        return "TokenBindingMessage{" +
                "tokenBindings=" + tokenBindings +
                '}';
    }
}
