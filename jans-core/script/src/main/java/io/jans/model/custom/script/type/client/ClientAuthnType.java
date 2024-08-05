package io.jans.model.custom.script.type.client;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface ClientAuthnType extends BaseExternalType {

    /**
     * Performs client authentication.
     *
     * @param context external client authentication context - io.jans.as.server.service.external.context.ExternalClientAuthnContext
     * @return authenticated client - io.jans.as.common.model.registration.Client
     */
    Object authenticateClient(Object context);
}
