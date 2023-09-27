package io.jans.kc.spi.auth;

import java.util.List;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public enum JansAuthenticatorConfigProp {
    
    SERVER_URL(
        "jans.auth.server.url",
        "Janssen Server Url",
        "Url of the Janssen Server",
        ProviderConfigProperty.STRING_TYPE,
        null,
        false),
    CLIENT_ID(
        "jans.auth.client.id",
        "Janssen Client ID",
        "Client ID of the OpenID Client created in Janssen-Auth",
        ProviderConfigProperty.STRING_TYPE,
        null,
        false),
    CLIENT_SECRET(
        "jans.auth.client.secret",
        "Janssen Client Secret",
        "Secret/Password of the OpenID Client created in Janssen-Auth",
        ProviderConfigProperty.PASSWORD,
        null,
        true
    ),
    ISSUER(
        "jans.auth.issuer",
        "Janssen OpenID Issuer(Optional)",
        "OpenID issuer of the Janssen server (Optional)",
        ProviderConfigProperty.STRING_TYPE,
        null,
        false ),
    EXTRA_SCOPES(
        "jans.auth.extra_scopes",
        "Extra OpenID Scopes",
        "Comma delimited list of extra OpenID scopes",
        ProviderConfigProperty.STRING_TYPE,
        null,
        false
    );

    private String name;
    private ProviderConfigProperty config;
    private JansAuthenticatorConfigProp(String name, String label, String helptext, String type, Object defaultvalue, boolean secret) {

        this.name = name;
        this.config = new ProviderConfigProperty(name, label, helptext, type, defaultvalue, secret);
    }

    public String getName() {

        return this.name;
    }

    public ProviderConfigProperty getConfig() {

        return this.config;
    }

    public static final List<ProviderConfigProperty> asList() {

        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        for(JansAuthenticatorConfigProp prop : values()) {
             builder.property(prop.config);
        }
        return builder.build();
    }
}
