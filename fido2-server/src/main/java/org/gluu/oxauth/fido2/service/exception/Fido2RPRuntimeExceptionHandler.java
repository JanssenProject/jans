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

package org.gluu.oxauth.fido2.service.exception;

import javax.inject.Inject;

import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.slf4j.Logger;
import org.xdi.service.exception.ExceptionHandler;

public class Fido2RPRuntimeExceptionHandler {

    @Inject
    private Logger log;

    @ExceptionHandler(value = { Fido2RPRuntimeException.class })
    public void handleException(Fido2RPRuntimeException ex) {
        log.error("Get exception: {}", ex.getFormattedMessage().getErrorMessage(), ex);
        // return handleExceptionInternal(ex, ex.getFormattedMessage(), new
        // HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
        // TODO: Finish Fido2RPRuntimeException exception handling
    }
}
