package io.jans.model.custom.script.type.authzchallenge;

import io.jans.model.custom.script.type.BaseExternalType;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public interface AuthorizationChallengeType extends BaseExternalType {

    boolean authorize(Object context);

    Map<String, String> getAuthenticationMethodClaims(Object context);

    // prepare authzRequest - AuthzRequest class
    void prepareAuthzRequest(Object context);
}
