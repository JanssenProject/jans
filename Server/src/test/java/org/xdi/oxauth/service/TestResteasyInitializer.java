/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.xdi.oxauth.authorize.ws.rs.AuthorizeRestWebServiceImpl;
import org.xdi.oxauth.clientinfo.ws.rs.ClientInfoRestWebServiceImpl;
import org.xdi.oxauth.gluu.ws.rs.GluuConfigurationWS;
import org.xdi.oxauth.idgen.ws.rs.IdGenRestWebService;
import org.xdi.oxauth.introspection.ws.rs.IntrospectionWebService;
import org.xdi.oxauth.jwk.ws.rs.JwkRestWebServiceImpl;
import org.xdi.oxauth.register.ws.rs.RegisterRestWebServiceImpl;
import org.xdi.oxauth.session.ws.rs.EndSessionRestWebServiceImpl;
import org.xdi.oxauth.token.ws.rs.TokenRestWebServiceImpl;
import org.xdi.oxauth.uma.ws.rs.*;
import org.xdi.oxauth.userinfo.ws.rs.UserInfoRestWebServiceImpl;

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
        classes.add(IdGenRestWebService.class);

        classes.add(CreateRptWS.class);
        classes.add(PermissionRegistrationWS.class);
        classes.add(ResourceSetRegistrationWS.class);
        classes.add(RptPermissionAuthorizationWS.class);
        classes.add(RptStatusWS.class);
        classes.add(ScopeWS.class);
        classes.add(UmaConfigurationWS.class);

        classes.add(GluuConfigurationWS.class);
        return classes;
    }

}