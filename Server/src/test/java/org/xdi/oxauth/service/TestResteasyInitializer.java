package org.xdi.oxauth.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.xdi.oxauth.authorize.ws.rs.AuthorizeRestWebServiceImpl;
import org.xdi.oxauth.gluu.ws.rs.GluuConfigurationWS;
import org.xdi.oxauth.token.ws.rs.TokenRestWebServiceImpl;
import org.xdi.oxauth.uma.ws.rs.PermissionRegistrationWS;
import org.xdi.oxauth.uma.ws.rs.ResourceSetRegistrationWS;
import org.xdi.oxauth.uma.ws.rs.RptPermissionAuthorizationWS;
import org.xdi.oxauth.uma.ws.rs.RptStatusWS;
import org.xdi.oxauth.uma.ws.rs.ScopeWS;
import org.xdi.oxauth.uma.ws.rs.UmaConfigurationWS;

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