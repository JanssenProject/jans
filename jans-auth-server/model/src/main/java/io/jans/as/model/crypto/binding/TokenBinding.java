/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.binding;

import java.util.Arrays;

/**
 * struct {
 * TokenBindingType tokenbinding_type;
 * TokenBindingID tokenbindingid;
 * opaque signature&lt;64..2^16-1&gt;;  Signature over the concatenation
 * of tokenbinding_type,
 * key_parameters and exported
 * keying material (EKM)
 * TB_Extension extensions&lt;0..2^16-1&gt;;
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
