package io.jans.configapi.plugin.saml.configuration.kc;

import io.jans.util.exception.InvalidConfigurationException;
import io.jans.configapi.plugin.saml.service.SamlConfigService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.keycloak.admin.client.Keycloak;

@ApplicationScoped
public class KeycloakConfig {

    private static Logger log = LoggerFactory.getLogger(KeycloakConfig.class);

    @Inject
    SamlConfigService samlConfigService;

    public Keycloak getInstance() {
        log.info("Keycloak instance entry - samlConfigService:{}", samlConfigService);
        
        if(samlConfigService==null || samlConfigService.find()==null) {
           throw new InvalidConfigurationException("Cannot create Keycloak Instance as SAML Config is null!");
        }

        log.trace("Keycloak instance entry - samlConfigService.find():{}", samlConfigService.find());

        return getInstance(samlConfigService.getServerUrl(), samlConfigService.getRealm(),
                samlConfigService.getUsername(), samlConfigService.getPassword(), samlConfigService.getClientId(),
                samlConfigService.getClientSecret());
    }

    public Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId,
            String clientSecret) {
        log.info(
                "Keycloak instance param serverUrl:{}, realm:{}, username:{}, password:{}, clientId:{}, clientSecret:{} ",
                serverUrl, realm, username, password, clientId, clientSecret);
        Keycloak keycloak = Keycloak.getInstance(serverUrl, realm, username, password, clientId, clientSecret);
        log.info("keycloak:{} ", keycloak);
        return keycloak;
    }

}
