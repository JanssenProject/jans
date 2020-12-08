/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.service.processor.assertion;

import java.util.EnumMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.service.processors.AssertionFormatProcessor;

@ApplicationScoped
public class AssertionProcessorFactory {

    private Map<AttestationFormat, AssertionFormatProcessor> processorsMap;

    @Inject
    private void initCommandProcessors(@Any Instance<AssertionFormatProcessor> assertionFormatProcessors) {
        this.processorsMap = new EnumMap<>(AttestationFormat.class);
        for (AssertionFormatProcessor app : assertionFormatProcessors) {
            processorsMap.put(app.getAttestationFormat(), app);
        }
    }

    public AssertionFormatProcessor getCommandProcessor(String fmtFormat) {
        try {
            AttestationFormat attestationFormat = AttestationFormat.valueOf(fmtFormat.replace('-', '_'));
            return processorsMap.get(attestationFormat);
        } catch (Exception e) {
            throw new Fido2RuntimeException("Unsupported format " + e.getMessage());
        }
    }
}
