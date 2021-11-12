/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.configuration.jetty;


import io.jans.util.StringHelper;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.Properties;

import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppLifeCycle;
import org.eclipse.jetty.deploy.graph.Node;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;

@ApplicationScoped
@Alternative
@Priority(1)
public class CommonExtraClasspathBinding implements AppLifeCycle.Binding
{
    private List<File> extraClasspath = new ArrayList<>();

    public String[] getBindingTargets()
    {
        return new String[] { "deploying" };
    }

    public void addAllJars(File dir)
    {
        for (File file : dir.listFiles())
        {
            if (!file.isFile())
            {
                continue;
            }
            if (file.getName().toLowerCase(Locale.ENGLISH).equals(".jar"))
            {
                addJar(file);
            }
        }
    }

    public void addJar(File jar)
    {
        if (jar.exists() && jar.isFile())
        {
            extraClasspath.add(jar);
        }
    }

    public void processBinding(Node node, App app) throws Exception
    {
        ContextHandler handler = app.getContextHandler();
        if (handler == null)
        {
            throw new NullPointerException("No Handler created for App: " + app);
        }

        if (handler instanceof WebAppContext)
        {
            WebAppContext webapp = (WebAppContext)handler;

            StringBuilder xtraCp = new StringBuilder();
            boolean delim = false;
            for (File cp : extraClasspath)
            {
                if (delim)
                {
                    xtraCp.append(File.pathSeparatorChar);
                }
                xtraCp.append(cp.getAbsolutePath());
                delim = true;
            }

            webapp.setExtraClasspath(xtraCp.toString());
        }
    }
}