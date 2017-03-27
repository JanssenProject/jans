package org.xdi.oxauth.util;

import java.io.File;

import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.xdi.oxauth.gluu.ws.rs.GluuConfigurationWS;
import org.xdi.oxauth.servlet.OpenIdConfiguration;
import org.xdi.oxauth.token.ws.rs.TokenRestWebService;

/**
 * Creates a deployment from a build Web Archive using ShrinkWrap ZipImporter
 * 
 * @author Yuriy Movchan
 */
public class Deployments {
	private static final String ARCHIVE_NAME = "oxauth.war";
	private static final String BUILD_DIRECTORY = "target";

	public static WebArchive createDeployment() {
		final WebArchive war = ShrinkWrap.create(WebArchive.class, "oxauth.war")
	         // adding the configuration class silences the logged exception when building the configuration on the server-side, but shouldn't be necessary
	         //.addClass(JettyEmbeddedConfiguration.class)
	            .addAsLibraries(
	                    Maven.configureResolver()
	                        .workOffline()
	                        .loadPomFromFile("pom.xml")
	                        .resolve("org.jboss.weld.servlet:weld-servlet-core")
	                        .withTransitivity()
	                        .as(GenericArchive.class))
	            .addAsLibraries(
	                    Maven.configureResolver()
	                        .workOffline()
	                        .loadPomFromFile("pom.xml")
	                        .resolve("org.jboss.resteasy:resteasy-servlet-initializer")
	                        .withTransitivity()
	                        .as(GenericArchive.class))
//	            .addPackage(GluuConfigurationWS.class.getPackage())
//	            .addPackage(OpenIdConfiguration.class.getPackage())
//	            .addPackages(true, "org.xdi.oxauth")
	         .addAsWebInfResource("jetty-env.xml")
	         .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
	         .addAsResource("arquillian.xml")
	         .setWebXML("in-container-web.xml");
	      return war;
		}

}
