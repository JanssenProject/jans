package org.xdi.oxauth.util;

import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Creates a deployment from a build Web Archive using ShrinkWrap ZipImporter
 * 
 * @author Yuriy Movchan
 */
@ArquillianSuiteDeployment
public class Deployments {

	public static WebArchive createDeployment() {
		final WebArchive war = ShrinkWrap.create(WebArchive.class, "oxauth.war")
				// adding the configuration class silences the logged exception
				// when building the configuration on the server-side, but
				// shouldn't be necessary
				// .addClass(JettyEmbeddedConfiguration.class)
				// Resteasy services
				// .addClass(ResteasyInitializer.class)
				// .addPackage(GluuConfigurationWS.class.getPackage())
				// Servlets
				.addAsWebInfResource("jetty-env.xml").addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				.setWebXML("web.xml");
		return war;
	}

}
