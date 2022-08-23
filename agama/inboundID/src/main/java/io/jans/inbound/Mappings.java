package io.jans.inbound;

import java.util.function.UnaryOperator;
import java.util.Map;

/**
 * Fields of this class can be referenced in the config properties of flow ExternalSiteLogin
 * (see the flow docs).
 */
public final class Mappings {

    public static final UnaryOperator<Map<String, Object>>

        GOOGLE = profile -> Map.of(
            Attrs.UID, "google-" + profile.get("sub"),
            Attrs.MAIL, profile.get("email"),
            Attrs.CN, profile.get("name"),
            Attrs.SN, profile.get("family_name"),
            Attrs.DISPLAY_NAME, profile.get("given_name"),
            Attrs.GIVEN_NAME, profile.get("given_name")
        );

    public static final UnaryOperator<Map<String, Object>>
    //See https://developers.facebook.com/docs/graph-api/reference/user

        FACEBOOK = profile -> Map.of(
            Attrs.UID, "facebook-" + profile.get("id"),
            Attrs.MAIL, profile.get("email"),
            Attrs.CN, profile.get("name"),
            Attrs.SN, profile.get("last_name"),
            Attrs.DISPLAY_NAME, profile.get("first_name"),
            Attrs.GIVEN_NAME, profile.get("first_name")
        );

    public static final UnaryOperator<Map<String, Object>>

        APPLE = profile -> Map.of(
            Attrs.UID, "apple-" + profile.get("sub"),
            Attrs.MAIL, profile.get("email"),
            Attrs.DISPLAY_NAME, profile.get("name"),
            Attrs.GIVEN_NAME, profile.get("name")
        );

    public static final UnaryOperator<Map<String, Object>>
    //See https://docs.github.com/en/rest/users/users

        GITHUB = profile -> Map.of(
            Attrs.UID, "github-" + profile.getOrDefault("login", profile.get("id")),
            Attrs.MAIL, profile.get("email"),
            Attrs.DISPLAY_NAME, profile.get("name"),
            Attrs.GIVEN_NAME, profile.get("name")
        );

    private Mappings() { }

}
