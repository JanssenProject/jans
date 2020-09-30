package org.gluu.configapi.service;

import org.gluu.oxauth.client.OpenIdConfigurationClient;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;
import org.gluu.util.StringHelper;
import org.gluu.util.exception.ConfigurationException;

import java.io.IOException;
import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

@ApplicationScoped
@Named("openIdService")
public class OpenIdService implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    public static final String WELL_KNOWN_OPENID_PATH = "/.well-known/openid-configuration";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;
    
    private OpenIdConfigurationResponse openIdConfiguration;

    public OpenIdService() {
        try {
            loadOpenIdConfiguration();
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to load oxAuth OpenId configuration");
        }               
    }
   
    private void loadOpenIdConfiguration()  throws IOException {
        String openIdProvider = configurationService.find().getIssuer();
        if (StringHelper.isEmpty(openIdProvider)) {
            throw new ConfigurationException("OpenIdProvider Url is invalid");
        }

        openIdProvider = openIdProvider + WELL_KNOWN_OPENID_PATH;

        final OpenIdConfigurationClient openIdConfigurationClient = new OpenIdConfigurationClient(openIdProvider);
        final OpenIdConfigurationResponse response = openIdConfigurationClient.execOpenIdConfiguration();
        if ((response == null) || (response.getStatus() != 200)) {
            throw new ConfigurationException("Failed to load oxAuth OpenId configuration");
        }

        logger.info("Successfully loaded oxAuth configuration");

        this.openIdConfiguration = response;
    }

    public OpenIdConfigurationResponse getOpenIdConfiguration() {
        return openIdConfiguration;
    }
  
    

}
