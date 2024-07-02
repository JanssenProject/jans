package io.jans.webauthn.util;

import java.util.List;

import io.jans.webauthn.models.PublicKeyCredentialSource;

public interface CredentialSelector {
    public PublicKeyCredentialSource selectFrom(List<PublicKeyCredentialSource> credentialList);
}
