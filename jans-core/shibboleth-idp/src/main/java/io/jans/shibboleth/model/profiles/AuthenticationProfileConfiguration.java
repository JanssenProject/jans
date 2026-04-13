package io.jans.shibboleth.model.profiles;

import java.util.List;


public abstract class AuthenticationProfileConfiguration extends BaseProfileConfiguration {


    private final List<String> defaultAuthenticationMethods;
    private final boolean forceAuthn;
    private final int maxAge;
    
}