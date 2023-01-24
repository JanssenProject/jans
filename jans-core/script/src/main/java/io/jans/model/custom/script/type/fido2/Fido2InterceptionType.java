/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.fido2;

import io.jans.model.custom.script.type.BaseExternalType;

public interface Fido2InterceptionType extends BaseExternalType {

    boolean interceptRegisterAttestation(Object paramAsJsonNode, Object context);
    boolean interceptVerifyAttestation(Object paramAsJsonNode, Object context);
    boolean interceptAuthenticateAssertion(Object paramAsJsonNode, Object context);
    boolean interceptVerifyAssertion(Object paramAsJsonNode, Object context);
}
