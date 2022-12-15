/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.server.authorize.ws.rs.AuthorizeRestWebServiceImpl;
import io.jans.as.server.authorize.ws.rs.DeviceAuthorizationRestWebServiceImpl;
import io.jans.as.server.bcauthorize.ws.rs.BackchannelAuthorizeRestWebServiceImpl;
import io.jans.as.server.bcauthorize.ws.rs.BackchannelDeviceRegistrationRestWebServiceImpl;
import io.jans.as.server.clientinfo.ws.rs.ClientInfoRestWebServiceImpl;
import io.jans.as.server.introspection.ws.rs.IntrospectionWebService;
import io.jans.as.server.jans.ws.rs.JansConfigurationWS;
import io.jans.as.server.jwk.ws.rs.JwkRestWebServiceImpl;
import io.jans.as.server.par.ws.rs.ParRestWebService;
import io.jans.as.server.register.ws.rs.RegisterRestWebServiceImpl;
import io.jans.as.server.revoke.RevokeRestWebServiceImpl;
import io.jans.as.server.revoke.RevokeSessionRestWebService;
import io.jans.as.server.session.ws.rs.CheckSessionStatusRestWebServiceImpl;
import io.jans.as.server.session.ws.rs.EndSessionRestWebServiceImpl;
import io.jans.as.server.session.ws.rs.SessionRestWebService;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceImpl;
import io.jans.as.server.token.ws.rs.TokenRestWebServiceImpl;
import io.jans.as.server.uma.ws.rs.UmaGatheringWS;
import io.jans.as.server.uma.ws.rs.UmaMetadataWS;
import io.jans.as.server.uma.ws.rs.UmaPermissionRegistrationWS;
import io.jans.as.server.uma.ws.rs.UmaResourceRegistrationWS;
import io.jans.as.server.uma.ws.rs.UmaRptIntrospectionWS;
import io.jans.as.server.uma.ws.rs.UmaScopeIconWS;
import io.jans.as.server.uma.ws.rs.UmaScopeWS;
import io.jans.as.server.userinfo.ws.rs.UserInfoRestWebServiceImpl;
import io.jans.as.server.ws.rs.fido.u2f.U2fAuthenticationWS;
import io.jans.as.server.ws.rs.fido.u2f.U2fConfigurationWS;
import io.jans.as.server.ws.rs.fido.u2f.U2fRegistrationWS;
import io.jans.as.server.ws.rs.stat.StatWS;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Integration with Resteasy
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/21/2017
 */
@ApplicationPath("/restv1")
public class ResteasyInitializer extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();
        classes.add(JansConfigurationWS.class);

        classes.add(AuthorizeRestWebServiceImpl.class);
        classes.add(RegisterRestWebServiceImpl.class);
        classes.add(ClientInfoRestWebServiceImpl.class);
        classes.add(RevokeRestWebServiceImpl.class);
        classes.add(RevokeSessionRestWebService.class);
        classes.add(JwkRestWebServiceImpl.class);
        classes.add(IntrospectionWebService.class);
        classes.add(ParRestWebService.class);
        classes.add(SessionRestWebService.class);

        classes.add(TokenRestWebServiceImpl.class);
        classes.add(UserInfoRestWebServiceImpl.class);
        classes.add(EndSessionRestWebServiceImpl.class);

        classes.add(UmaMetadataWS.class);
        classes.add(UmaGatheringWS.class);
        classes.add(UmaPermissionRegistrationWS.class);
        classes.add(UmaResourceRegistrationWS.class);
        classes.add(UmaRptIntrospectionWS.class);
        classes.add(UmaScopeIconWS.class);
        classes.add(UmaScopeWS.class);

        classes.add(U2fConfigurationWS.class);
        classes.add(U2fAuthenticationWS.class);
        classes.add(U2fRegistrationWS.class);

        classes.add(CheckSessionStatusRestWebServiceImpl.class);

        classes.add(DeviceAuthorizationRestWebServiceImpl.class);
        classes.add(BackchannelAuthorizeRestWebServiceImpl.class);
        classes.add(BackchannelDeviceRegistrationRestWebServiceImpl.class);

        classes.add(StatWS.class);

        classes.add(SsaRestWebServiceImpl.class);

        return classes;
    }

}