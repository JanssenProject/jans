/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.server.authorize.ws.rs.AuthorizeRestWebServiceImpl;
import io.jans.as.server.clientinfo.ws.rs.ClientInfoRestWebServiceImpl;
import io.jans.as.server.introspection.ws.rs.IntrospectionWebService;
import io.jans.as.server.jans.ws.rs.JansConfigurationWS;
import io.jans.as.server.jwk.ws.rs.JwkRestWebServiceImpl;
import io.jans.as.server.register.ws.rs.RegisterRestWebServiceImpl;
import io.jans.as.server.session.ws.rs.EndSessionRestWebServiceImpl;
import io.jans.as.server.token.ws.rs.TokenRestWebServiceImpl;
import io.jans.as.server.uma.ws.rs.UmaGatheringWS;
import io.jans.as.server.uma.ws.rs.UmaMetadataWS;
import io.jans.as.server.uma.ws.rs.UmaPermissionRegistrationWS;
import io.jans.as.server.uma.ws.rs.UmaResourceRegistrationWS;
import io.jans.as.server.uma.ws.rs.UmaRptIntrospectionWS;
import io.jans.as.server.uma.ws.rs.UmaScopeWS;
import io.jans.as.server.userinfo.ws.rs.UserInfoRestWebServiceImpl;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.Set;

/**
 * Integration with Resteasy
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version April 26, 2017
 */
@Provider
public class TestResteasyInitializer extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(AuthorizeRestWebServiceImpl.class);
        classes.add(TokenRestWebServiceImpl.class);
        classes.add(RegisterRestWebServiceImpl.class);
        classes.add(UserInfoRestWebServiceImpl.class);
        classes.add(IntrospectionWebService.class);
        classes.add(ClientInfoRestWebServiceImpl.class);
        classes.add(JwkRestWebServiceImpl.class);
        classes.add(EndSessionRestWebServiceImpl.class);

        classes.add(UmaPermissionRegistrationWS.class);
        classes.add(UmaResourceRegistrationWS.class);
        classes.add(UmaRptIntrospectionWS.class);
        classes.add(UmaScopeWS.class);
        classes.add(UmaMetadataWS.class);
        classes.add(UmaGatheringWS.class);

        classes.add(JansConfigurationWS.class);
        return classes;
    }

}