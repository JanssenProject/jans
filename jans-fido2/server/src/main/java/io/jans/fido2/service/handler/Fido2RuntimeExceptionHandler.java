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

package io.jans.fido2.service.handler;

import io.jans.service.exception.ExceptionHandler;
import io.jans.fido2.exception.Fido2RuntimeException;
import org.slf4j.Logger;

import jakarta.inject.Inject;

public class Fido2RuntimeExceptionHandler {

    @Inject
    private Logger log;

    @ExceptionHandler(value = { Fido2RuntimeException.class })
    public void handleException(Fido2RuntimeException ex) {
        log.error("Get exception: {}", ex.getFormattedMessage().getErrorMessage(), ex);
        // return handleExceptionInternal(ex, ex.getFormattedMessage(), new
        // HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
        // TODO: Finish Fido2RuntimeException exception handling
    }
}
