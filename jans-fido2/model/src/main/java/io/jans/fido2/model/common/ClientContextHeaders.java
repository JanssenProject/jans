/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.model.common;

/**
 * Headers carrying the end user's connection details across a server-to-server hop.
 *
 * The FIDO2 endpoints are never called by the browser - the Authorization Server, Casa
 * and the person-authentication script relay to them - so the only client the server can
 * observe on its own is the calling service. A caller that does hold the browser's request
 * sends these headers so metrics describe the user rather than the relay.
 *
 * They are only honoured from a caller listed in {@code fido2TrustedClientContextSources};
 * anywhere else they are ordinary request headers and are ignored.
 *
 * @author Janssen Project
 */
public final class ClientContextHeaders {

    /**
     * End user's IP address, as observed by the calling service.
     */
    public static final String CLIENT_IP = "X-Jans-Client-IP";

    /**
     * End user's browser user agent, as observed by the calling service.
     */
    public static final String CLIENT_USER_AGENT = "X-Jans-Client-User-Agent";

    private ClientContextHeaders() {
    }
}
