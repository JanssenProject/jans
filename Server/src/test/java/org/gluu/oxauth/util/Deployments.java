package org.gluu.oxauth.util;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.File;

/**
 * Creates a deployment from a build Web Archive using ShrinkWrap ZipImporter
 * 
 * @author Yuriy Movchan
 */
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
	    try {
            File dir = new File("src/main/webapp");
            addFiles(war, dir);
        } catch (Exception e) {
	        e.printStackTrace();
        }

	    return war;
	}

    private static void addFiles(WebArchive war, File dir) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                war.addAsWebResource(f, f.getPath().replace("\\", "/").substring("src/main/webapp/".length()));
            } else {
                addFiles(war, f);
            }
        }
    }

}
