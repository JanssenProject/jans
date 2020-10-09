/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.gluu.service.config.ConfigurationFactory;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

/**
 * Provides operations with velocity templates
 * 
 * @author Yuriy Movchan Date: 12.15.2010
 */
@ApplicationScoped
public class TemplateService implements Serializable {

	private static final long serialVersionUID = 4898430090669045605L;

	@Inject
	private Logger log;

	/*
	 * Generate relying-party.xml using relying-party.xml.vm template
	 */
	public String generateConfFile(String template, VelocityContext context) {
		StringWriter sw = new StringWriter();
		try {
			Velocity.mergeTemplate(template + ".vm", "UTF-8", context, sw);
		} catch (Exception ex) {
			log.error("Failed to load velocity template '{}'", template, ex);
			return null;
		}

		return sw.toString();
	}

	public boolean writeConfFile(String confFile, String conf) {
		try {
			FileUtils.writeStringToFile(new File(confFile), conf, "UTF-8");
		} catch (IOException ex) {
			log.error("Failed to write IDP configuration file '{}'", confFile, ex);
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	/*
	 * Load Velocity configuration from classpath
	 */
	private Properties getTemplateEngineConfiguration() {
		Properties properties = new Properties();
		try (InputStream is = TemplateService.class.getClassLoader().getResourceAsStream("velocity.properties");) {
			properties.load(is);
			String loaderType = properties.getProperty("resource.loader").trim();
			if (loaderType.contains("jar")) {
				properties = loadFromJar(properties);
			}
			if (loaderType.contains("class_path")) {
				properties = loadFromPathClasspath(properties);
			}
			if (loaderType.contains("file")) {
				properties = loadFromFileSystem(properties);
			}
		} catch (IOException ex) {
			log.error("Failed to load velocity.properties", ex);
		}
		return properties;
	}

	private Properties loadFromJar(Properties properties) {
		for (URL url : ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs()) {
			if (url.getPath().contains("oxtrust-configuration")) {
				String oxtrustConfigurationJar = "jar:file:" + url.getPath();
				properties.setProperty("jar.resource.loader.path", oxtrustConfigurationJar + ",");
				break;
			}
		}
		log.debug("jar path =" + properties.getProperty("jar.resource.loader.path"));
		return properties;
	}

	private Properties loadFromFileSystem(Properties properties) {
		String idpTemplatesLocation = getTemplatesLocation();
		String pathes = getTemplatePathes(idpTemplatesLocation);

		properties.setProperty("file.resource.loader.path", pathes);
		log.debug("file.resource.loader.path = " + pathes);
		return properties;
	}

	private Properties loadFromPathClasspath(Properties properties) {
		String pathes = getTemplatePathes("META-INF/");

		properties.setProperty("class_path.resource.loader.path", pathes);
		log.debug("class_path.resource.loader.path = " + pathes);
		return properties;
	}

	private String getTemplatePathes(String idpTemplatesLocation) {
		String folder1 = idpTemplatesLocation + "shibboleth3" + File.separator + "idp";
		String folder2 = idpTemplatesLocation + "shibboleth3" + File.separator + "sp";
		String folder3 = idpTemplatesLocation + "ldif";
		String folder4 = idpTemplatesLocation + "shibboleth3" + File.separator + "idp" + File.separator
				+ "MetadataFilter";
		String folder5 = idpTemplatesLocation + "shibboleth3" + File.separator + "idp" + File.separator
				+ "ProfileConfiguration";
		String folder6 = idpTemplatesLocation + "template" + File.separator + "conf";
		String folder7 = idpTemplatesLocation + "template" + File.separator + "shibboleth3";
		
		String pathes = folder1 + ", " + folder2 + ", " + folder3 + ", " + folder4
				+ ", " + folder5 + ", " + folder6 + ", " + folder7;
		return pathes;
	}

	public List<String> getTemplateNames(String baseFolder) {
		String classpathIdpTemplatesLocation = "META-INF";
		List<String> classpathTemplateNames = getClasspathTemplateNames(classpathIdpTemplatesLocation + "/" + baseFolder);

		String fileIdpTemplatesLocation = getTemplatesLocation();
		List<String> filesystemTemplateNames = getFilesystemTemplateNames(fileIdpTemplatesLocation + "/" + baseFolder);
		
		Set<String> merged = new HashSet<String>();
		merged.addAll(classpathTemplateNames);
		merged.addAll(filesystemTemplateNames);
		
		return new ArrayList<String>(merged);
	}

	public List<String> getClasspathTemplateNames(String baseFolder) {
		List<String> names = new ArrayList<String>();

		URL url = TemplateService.class.getClassLoader().getResource(baseFolder);

		if (url == null) {
			return names;
		}

		JarURLConnection urlCon;
		try {
			urlCon = (JarURLConnection) (url.openConnection());
		} catch (IOException ex) {
			log.error("Failed to read jar content: %s", url, ex);
			
			return names;
		}

		try (JarFile jar = urlCon.getJarFile();) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				String entry = entries.nextElement().getName();
				if (entry.startsWith(baseFolder)) {
					if (!entry.substring(baseFolder.length()).equals("/")) {
						File file = new File(entry);
						names.add(file.getName());
					}
				}
			}
		} catch (IOException ex) {
			log.error("Failed to read jar content: %s", url, ex);
		}

		return names;
	}
	
	public List<String> getFilesystemTemplateNames(String baseFolder) {
		List<String> names = new ArrayList<String>();

		File baseFolderFile = new File(baseFolder);

		File[] foudFiles = null;

		if (baseFolderFile.exists() && baseFolderFile.isDirectory()) {
			foudFiles = baseFolderFile.listFiles();
		} else {
			return names;
		}

		for (File foundFile : foudFiles) {
			names.add(foundFile.getName());
		}
		
		return names;

	}

	/*
	 * Initialize singleton instance during startup
	 */
	public void initTemplateEngine() {
		try {
			Velocity.init(getTemplateEngineConfiguration());
		} catch (Exception ex) {
			log.error("Failed to initialize Velocity", ex);
		}
	}

	public String getTemplatesLocation() {
		String jetyBase = System.getProperty("jetty.base");

		if (StringHelper.isEmpty(jetyBase)) {
			return ConfigurationFactory.DIR;
		}

		return jetyBase + File.separator + "conf" + File.separator;
	}

}
