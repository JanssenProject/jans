/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.util;

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
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "jans-auth.war")
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
        final File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File f : files) {
            if (f.isFile()) {
                war.addAsWebResource(f, f.getPath().replace("\\", "/").substring("src/main/webapp/".length()));
            } else {
                addFiles(war, f);
            }
        }
    }

}
