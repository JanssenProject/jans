package org.gluu.configapi.auth;

import org.gluu.configapi.service.UmaService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.Serializable;

import org.slf4j.Logger;

@ApplicationScoped
@Named("umaAuthorizationService")
public class UmaAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma2-configuration";

    @Inject
    private Logger logger;

    @Inject
    UmaService umaService;
    
    public Response processAuthorization(String token, ResourceInfo resourceInfo) throws Exception {
        logger.info(" UmaAuthorizationService::processAuthorization() - token  = " + token
                + " , resourceInfo = " + resourceInfo+" , umaService = "+umaService);
        return null;
    }
   
}