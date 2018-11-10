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

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;

@ApplicationScoped
public class AssertionProcessorFactory {

    private Map<AttestationFormat, AssertionFormatProcessor> processorsMap;

    @PostConstruct
    public void create() throws Exception {
        this.processorsMap = new EnumMap<>(AttestationFormat.class);
    }

    @Inject
    public void setCommandProcessors(@Any Instance<AssertionFormatProcessor> assertionFormatProcessors) {
        for (AssertionFormatProcessor app : assertionFormatProcessors) {
            processorsMap.put(app.getAttestationFormat(), app);
        }
    }

    public AssertionFormatProcessor getCommandProcessor(String fmtFormat) {
        try {
            AttestationFormat attestationFormat = AttestationFormat.valueOf(fmtFormat.replace('-', '_'));
            return processorsMap.get(attestationFormat);
        } catch (Exception e) {
            throw new Fido2RPRuntimeException("Unsupported format " + e.getMessage());
        }
    }
}
