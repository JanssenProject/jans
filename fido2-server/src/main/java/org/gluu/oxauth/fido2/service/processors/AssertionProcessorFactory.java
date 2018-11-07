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

package org.gluu.oxauth.fido2.service.processors;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.service.Fido2RPRuntimeException;

@Named
public class AssertionProcessorFactory {
    private final Map<AttestationFormat, AssertionFormatProcessor> processorsMap = new EnumMap<>(AttestationFormat.class);

    @Inject
    public void setCommandProcessors(List<AssertionFormatProcessor> assertionFormatProcessors) {
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
