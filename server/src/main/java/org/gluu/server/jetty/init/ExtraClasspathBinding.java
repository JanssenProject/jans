package org.gluu.server.jetty.init;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppLifeCycle;
import org.eclipse.jetty.deploy.graph.Node;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;

public class ExtraClasspathBinding implements AppLifeCycle.Binding {
	private List<File> extraClasspath = new ArrayList<>();

	public String[] getBindingTargets() {
		return new String[] { "deploying" };
	}

	public void addAllJars(File dir) {
		for (File file : dir.listFiles()) {
			if (!file.isFile()) {
				continue;
			}
			if (file.getName().toLowerCase(Locale.ENGLISH).equals(".jar")) {
				addJar(file);
			}
		}
	}

	public void addJar(File jar) {
		if (jar.exists() && jar.isFile()) {
			extraClasspath.add(jar);
		}
	}

	public void processBinding(Node node, App app) throws Exception {
		ContextHandler handler = app.getContextHandler();
		if (handler == null) {
			throw new NullPointerException("No Handler created for App: " + app);
		}

		if (handler instanceof WebAppContext) {
			WebAppContext webapp = (WebAppContext) handler;

			StringBuilder xtraCp = new StringBuilder();
			boolean delim = false;
			for (File cp : extraClasspath) {
				if (delim) {
					xtraCp.append(File.pathSeparatorChar);
				}
				xtraCp.append(cp.getAbsolutePath());
				delim = true;
			}

			webapp.setExtraClasspath(xtraCp.toString());
		}
	}
}