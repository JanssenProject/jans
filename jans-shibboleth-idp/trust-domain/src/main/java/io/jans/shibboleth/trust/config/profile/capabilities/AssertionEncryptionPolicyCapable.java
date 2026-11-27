package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;

public interface AssertionEncryptionPolicyCapable {

    AssertionEncryptionPolicy assertionEncryptionPolicy();

    default AssertionEncryptionPolicy getAssertionEncryptionPolicy() {

        return assertionEncryptionPolicy();
    }
}
