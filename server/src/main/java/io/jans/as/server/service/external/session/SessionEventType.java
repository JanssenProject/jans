/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.session;

/**
 * @author Yuriy Zabrovarnyy
 */
public enum SessionEventType {
    AUTHENTICATED,
    UNAUTHENTICATED,
    UPDATED,
    GONE // it can be time out, or expired or ended by /end_session endpoint
}