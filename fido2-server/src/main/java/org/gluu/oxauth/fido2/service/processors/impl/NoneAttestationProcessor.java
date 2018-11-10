/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.service.processors.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.AuthData;
import org.gluu.oxauth.fido2.model.auth.CredAndCounterData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.service.CommonVerifiers;
import org.gluu.oxauth.fido2.service.processors.AttestationFormatProcessor;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class NoneAttestationProcessor implements AttestationFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.none;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData credential, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {
        log.info("None/Surrogate attestation {}", attStmt);
        if (attStmt.iterator().hasNext()) {
            throw new Fido2RPRuntimeException("Problem with None/Surrogate attestation");
        }
        credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());

    }
}
