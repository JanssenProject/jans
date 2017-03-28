package org.xdi.oxauth.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.xdi.oxauth.gluu.ws.rs.GluuConfigurationWS;

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
		classes.add(GluuConfigurationWS.class);
		return classes;
	}

}