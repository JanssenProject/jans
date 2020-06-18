package org.gluu.oxauth.service.external.session;

/**
 * @author Yuriy Zabrovarnyy
 */
public enum SessionEventType {
    AUTHENTICATED,
    UNAUTHENTICATED,
    UPDATED,
    GONE // it can be time out, or expired or ended by /end_session endpoint
}