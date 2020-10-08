package io.jans.configapi.service;

import io.jans.oxauth.model.uma.UmaMetadata;
import io.jans.util.exception.ConfigurationException;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

@ApplicationScoped
@Named("umaService")
public class UmaService implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma2-configuration";

    @Inject
    Logger logger;

    @Inject
    UmaService umaService;

    private UmaMetadata umaMetadata;
    
    public UmaService() {
        try {
            loadUmaConfiguration();
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to load oxAuth UMA configuration");
        }               
    }
   
    
    public UmaMetadata getUmaMetadata() throws Exception{
        return this.umaMetadata;
    }
    

    
    public UmaMetadata loadUmaConfiguration() throws Exception {
        return null;
    }




}
