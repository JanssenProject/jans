package io.jans.configapi.plugin.keycloak.idp.broker.configuration;

import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpAppConfiguration;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpConfigService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.keycloak.admin.client.Keycloak;

@ApplicationScoped
public class KeycloakConfig {

    private static Logger log = LoggerFactory.getLogger(KeycloakConfig.class);

    @Inject
    IdpConfigService idpConfigService;

    public IdpAppConfiguration getIdpAppConfiguration() {
        return idpConfigService.getIdpConf().getDynamicConf();
    }

    public Keycloak getInstance() {
        log.info("Keycloak instance entry - idpConfigService:{}, getIdpAppConfiguration()", idpConfigService,
                getIdpAppConfiguration());
        IdpAppConfiguration idpAppConfiguration = this.getIdpAppConfiguration();

        log.trace("Keycloak instance entry - idpAppConfiguration:{}", idpAppConfiguration);

        return getInstance(idpAppConfiguration.getServerUrl(), idpAppConfiguration.getRealm(),
                idpAppConfiguration.getUsername(), idpAppConfiguration.getPassword(), idpAppConfiguration.getClientId(),
                idpAppConfiguration.getClientSecret());
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
