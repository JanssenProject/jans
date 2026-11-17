/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.lock;

public record LockSettings(String lockBaseUrl, String authBaseUrl, String clientId, String clientSecret) {

    public static final String SCOPE_WRITE = "https://jans.io/oauth/lock/trace.write";
    public static final String SCOPE_READONLY = "https://jans.io/oauth/lock/trace.readonly";
    public static final String SCOPE_ADMIN = "https://jans.io/oauth/lock/trace.admin";

    public String apiBase() {
        return lockBaseUrl + "/jans-lock/api/v1";
    }

    public String tokenEndpoint() {
        return authBaseUrl + "/jans-auth/restv1/token";
    }

    public String wellKnownUri() {
        return lockBaseUrl + "/.well-known/lock-server-configuration";
    }

    @Override
    public String toString() {
        return "LockSettings[lockBaseUrl=" + lockBaseUrl + ", authBaseUrl=" + authBaseUrl + ", clientId=" + clientId
                + ", clientSecret=[REDACTED]]";
    }
}
