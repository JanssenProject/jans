/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.jetty;

import jakarta.servlet.http.HttpServlet;
import org.eclipse.jetty.servlet.*;


import java.io.*;
import java.util.*;
import jakarta.ws.rs.ApplicationPath;

import org.slf4j.*;
public class JansJettyServlet extends HttpServlet {
    
}
/*public class JansJettyUtil extends AppLifeCycle.Binding  {

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
}*/