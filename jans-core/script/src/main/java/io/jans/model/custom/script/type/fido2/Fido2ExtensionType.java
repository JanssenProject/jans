/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.fido2;

import io.jans.model.custom.script.type.BaseExternalType;

public interface Fido2ExtensionType extends BaseExternalType {

    boolean registerAttestationStart(Object paramAsJsonNode, Object context);
    boolean registerAttestationFinish(Object paramAsJsonNode, Object context);

    boolean verifyAttestationStart(Object paramAsJsonNode, Object context);
    boolean verifyAttestationFinish(Object paramAsJsonNode, Object context);

    boolean authenticateAssertionStart(Object paramAsJsonNode, Object context);
    boolean authenticateAssertionFinish(Object paramAsJsonNode, Object context);

    boolean verifyAssertionStart(Object paramAsJsonNode, Object context);
    boolean verifyAssertionFinish(Object paramAsJsonNode, Object context);
}
