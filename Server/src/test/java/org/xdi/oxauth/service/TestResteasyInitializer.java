package org.xdi.oxauth.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.xdi.oxauth.authorize.ws.rs.AuthorizeRestWebServiceImpl;
import org.xdi.oxauth.clientinfo.ws.rs.ClientInfoRestWebServiceImpl;
import org.xdi.oxauth.gluu.ws.rs.GluuConfigurationWS;
import org.xdi.oxauth.idgen.ws.rs.IdGenRestWebService;
import org.xdi.oxauth.introspection.ws.rs.IntrospectionWebService;
import org.xdi.oxauth.jwk.ws.rs.JwkRestWebService;
import org.xdi.oxauth.register.ws.rs.RegisterRestWebServiceImpl;
import org.xdi.oxauth.token.ws.rs.TokenRestWebServiceImpl;
import org.xdi.oxauth.token.ws.rs.ValidateTokenRestWebService;
import org.xdi.oxauth.uma.ws.rs.CreateRptWS;
import org.xdi.oxauth.uma.ws.rs.PermissionRegistrationWS;
import org.xdi.oxauth.uma.ws.rs.ResourceSetRegistrationWS;
import org.xdi.oxauth.uma.ws.rs.RptPermissionAuthorizationWS;
import org.xdi.oxauth.uma.ws.rs.RptStatusWS;
import org.xdi.oxauth.uma.ws.rs.ScopeWS;
import org.xdi.oxauth.uma.ws.rs.UmaConfigurationWS;
import org.xdi.oxauth.userinfo.ws.rs.UserInfoRestWebServiceImpl;

/**
 * Integration with Resteasy
 * 
 * @author Yuriy Movchan
 * @version 0.1, 03/28/2017
 */
@Provider
public class TestResteasyInitializer extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(AuthorizeRestWebServiceImpl.class);
		classes.add(TokenRestWebServiceImpl.class);
		classes.add(ValidateTokenRestWebService.class);
		classes.add(RegisterRestWebServiceImpl.class);
		classes.add(UserInfoRestWebServiceImpl.class);
		classes.add(IntrospectionWebService.class);
		classes.add(ClientInfoRestWebServiceImpl.class);
		classes.add(JwkRestWebService.class);
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