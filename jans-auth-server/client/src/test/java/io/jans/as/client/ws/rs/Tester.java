package io.jans.as.client.ws.rs;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * @author Yuriy Z
 */
public class Tester {

    public static final List<String> standardScopes = ImmutableList.of(
            "openid",
            "profile",
            "address",
            "email");

    public static final List<String> addressScopes = ImmutableList.of(
            "openid",
            "address");

    public static final List<String> clientInfoScopes = ImmutableList.of(
            "openid",
            "clientinfo");

    public static final List<String> testScopes = ImmutableList.of(
            "openid",
            "test");

    private Tester() {
    }
}
