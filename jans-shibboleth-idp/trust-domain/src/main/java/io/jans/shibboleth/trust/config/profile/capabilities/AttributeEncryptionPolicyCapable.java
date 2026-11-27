package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;

public interface AttributeEncryptionPolicyCapable {

    AttributeEncryptionPolicy attributeEncryptionPolicy();

    default AttributeEncryptionPolicy getAttributeEncryptionPolicy() {

        return attributeEncryptionPolicy();
    }
}
