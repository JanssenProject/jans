package io.jans.kc.spi.auth;

import java.util.List;

import org.jboss.logging.Logger;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;

import org.keycloak.Config;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import org.keycloak.provider.ProviderConfigProperty;

import io.jans.kc.spi.ProviderIDs;
import io.jans.kc.oidc.OIDCMetaCache;
import io.jans.kc.oidc.OIDCService;
import io.jans.kc.oidc.impl.HashBasedOIDCMetaCache;
import io.jans.kc.oidc.impl.NimbusOIDCService;


public class JansAuthenticatorFactory implements AuthenticatorFactory {
    
    private static final String PROVIDER_ID = ProviderIDs.JANS_AUTHENTICATOR_PROVIDER;
    
    private static final String DISPLAY_TYPE = "Janssen Authenticator";
    private static final String REFERENCE_CATEGORY = "Janssen Authenticator";
    private static final String HELP_TEXT= "Janssen authenticator for Keycloak";

    private static final Logger log = Logger.getLogger(JansAuthenticatorFactory.class);

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

    
    private static final OIDCMetaCache META_CACHE = new HashBasedOIDCMetaCache();
    private static final OIDCService OIDC_SERVICE = new NimbusOIDCService(META_CACHE);
    private static final JansAuthenticator INSTANCE = new JansAuthenticator(OIDC_SERVICE);

    @Override
    public String getId() {

        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {

        log.debug("Janssen authenticator create()");
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
        
        //nothing to do for now during initialization
    }

    @Override
    public void close() {

        //nothing to do for now during shutdown
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

        //nothing to do postInit
    }

    @Override
    public String getDisplayType() {

        return DISPLAY_TYPE;
    }

    @Override
    public String getReferenceCategory() {

        return REFERENCE_CATEGORY;
    }

    @Override
    public boolean isConfigurable() {

        return true;
    }

    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {

        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {

        return false;
    }

    @Override
    public String getHelpText() {

        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        return JansAuthenticatorConfigProp.asList();
    }
}
