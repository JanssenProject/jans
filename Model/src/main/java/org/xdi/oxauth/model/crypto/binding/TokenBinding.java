package org.xdi.oxauth.model.crypto.binding;

import java.util.Arrays;

/**
 * struct {
 *    TokenBindingType tokenbinding_type;
 *    TokenBindingID tokenbindingid;
 *    opaque signature<64..2^16-1>;  Signature over the concatenation
 *                                     of tokenbinding_type,
 *                                     key_parameters and exported
 *                                     keying material (EKM)
 *    TB_Extension extensions<0..2^16-1>;
 * } TokenBinding;
 *
 * @author Yuriy Zabrovarnyy
 */
public class TokenBinding {

    private TokenBindingType tokenBindingType;
    private TokenBindingID tokenBindingID;
    private byte[] signature;
    private TokenBindingExtension extension;

    public TokenBinding() {
    }

    public TokenBinding(TokenBindingType tokenBindingType, TokenBindingID tokenBindingID, byte[] signature, TokenBindingExtension extension) {
        this.tokenBindingType = tokenBindingType;
        this.tokenBindingID = tokenBindingID;
        this.signature = signature;
        this.extension = extension;
    }

    public TokenBindingType getTokenBindingType() {
        return tokenBindingType;
    }

    public TokenBindingID getTokenBindingID() {
        return tokenBindingID;
    }

    public byte[] getSignature() {
        return signature;
    }

    public TokenBindingExtension getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return "TokenBinding{" +
                "tokenBindingType=" + tokenBindingType +
                ", tokenBindingID=" + tokenBindingID +
                ", signature=" + Arrays.toString(signature) +
                ", extension=" + extension +
                '}';
    }
}
