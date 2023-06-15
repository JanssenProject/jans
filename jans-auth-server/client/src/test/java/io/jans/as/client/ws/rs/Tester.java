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

    private Tester() {
    }
}
