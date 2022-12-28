/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.external.session;

/**
 * @author Yuriy Zabrovarnyy
 */
public enum SessionEventType {
    AUTHENTICATED,
    UNAUTHENTICATED,
    UPDATED,
    GONE // it can be time out, or expired or ended by /end_session endpoint
}