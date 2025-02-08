/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.binding;

import java.util.Arrays;

/**
 * struct {
 * TB_ExtensionType extension_type;
 * opaque extension_data&lt;0..2^16-1&gt;;
 * } TB_Extension;
 *
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingExtension {

    private TokenBindingExtensionType extensionType;
    private byte[] extensionData;

    public TokenBindingExtension() {
    }

    public TokenBindingExtension(TokenBindingExtensionType extensionType, byte[] extensionData) {
        this.extensionType = extensionType;
        this.extensionData = extensionData;
    }

    public TokenBindingExtensionType getExtensionType() {
        return extensionType;
    }

    public byte[] getExtensionData() {
        return extensionData;
    }

    @Override
    public String toString() {
        return "TokenBindingExtension{" +
                "extensionType=" + extensionType +
                ", extensionData=" + Arrays.toString(extensionData) +
                '}';
    }
}
