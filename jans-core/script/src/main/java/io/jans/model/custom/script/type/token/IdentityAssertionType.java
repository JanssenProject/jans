package io.jans.model.custom.script.type.token;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Script interface for Identity Assertion JWT Authorization Grant (ID-JAG) interception.
 *
 * Spec: draft-ietf-oauth-identity-assertion-authz-grant
 *
 * @author Yuriy Z
 */
public interface IdentityAssertionType extends BaseExternalType {

    /**
     * Called after ID-JAG JWT claims are populated but before the token is signed.
     * Use this to add, remove, or modify claims in the ID-JAG payload.
     *
     * @param idJagAsJwt io.jans.as.model.jwt.Jwt — the unsigned ID-JAG JWT
     * @param context    io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true to accept claim changes, false to discard them
     */
    boolean modifyIdJagPayload(Object idJagAsJwt, Object context);

    /**
     * Called after the token exchange response JSON is built but before it is returned to the client.
     * Use this to add or modify top-level response fields.
     *
     * @param responseAsJsonObject org.json.JSONObject — the token exchange response
     * @param context              io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true to accept response changes, false to discard them
     */
    boolean modifyResponse(Object responseAsJsonObject, Object context);
}
