/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.customization;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;

import jakarta.faces.application.ApplicationConfigurationPopulator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.faces.util.FacesLogger;

/**
 * Created by eugeniuparvan on 5/1/17.
 */
public class FacesConfigPopulator extends ApplicationConfigurationPopulator {
	/**
	 * <p>
	 * /faces-config/navigation-rule
	 * </p>
	 */
	private static final String NAVIGATION_RULE = "navigation-rule";
	private static final String FACES_CONFIG_PATTERN = ".*\\.navigation\\.xml$";
	private static final String DEFAULT_NAVIGATION_PATH = "META-INF/navigation";

	private Logger log = LoggerFactory.getLogger(FacesConfigPopulator.class);

	@Override
	public void populateApplicationConfiguration(Document toPopulate) {
		populateNavigationRules(toPopulate);
	}

	// Navigation Rules
	protected void populateNavigationRules(Document toPopulate) {
		log.debug("Starting configuration populator");

		if (Utils.isCustomPagesDirExists()) {
			String customPath = Utils.getCustomPagesPath();
			log.debug("Adding navigation rules from custom dir folder: {}", customPath);
			try {
				findAndUpdateNavigationRules(toPopulate, customPath);
			} catch (Exception ex) {
				FacesLogger.CONFIG.getLogger().log(Level.SEVERE, "Can't add customized navigation rules");
			}
		}

		try {
			log.debug("Adding navigation rules from application resurces");
			Enumeration<URL> urlEnumeration = getClass().getClassLoader().getResources(DEFAULT_NAVIGATION_PATH);
			if (urlEnumeration.hasMoreElements()) {
				URL url = urlEnumeration.nextElement();
				findAndUpdateNavigationRules(toPopulate, url.getPath());
			}
		} catch (Exception ex) {
			log.error("Failed to populate application configuraton", ex);
		}
	}

	/**
	 * Recursively finds all *.navigation.xml files located in custom pages
	 * directory, and adds navigation rules to navigation handler
	 *
	 * @param path to custom pages directory
	 * @throws Exception
	 */
	private void findAndUpdateNavigationRules(Document toPopulate, String path) throws Exception {
		File file = new File(path);
		RegexFileFilter regexFileFilter = new RegexFileFilter(FACES_CONFIG_PATTERN);
		Collection<File> facesConfigFiles = FileUtils.listFiles(file, regexFileFilter, DirectoryFileFilter.DIRECTORY);
		log.debug("Found '{}' navigation rules files", facesConfigFiles.size());

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();

		for (File files : facesConfigFiles) {
			String faceConfig = files.getAbsolutePath();
			updateDocument(toPopulate, builder, faceConfig);
			log.debug("Added navigation rules from {}", faceConfig);
		}
	}

	/**
	 * Validates *.faces-config.xml file and creates DocumentInfo class
	 *
	 * @param docBuilder
	 * @param faceConfig
	 * @return
	 */
	private void updateDocument(Document toPopulate, DocumentBuilder docBuilder, String faceConfig) {
		try {
			Document document = docBuilder.parse(new File(faceConfig));
			Element root = toPopulate.getDocumentElement();
			NodeList navigationRules = getNavigationRules(document);
			for (int i = 0; i < navigationRules.getLength(); ++i) {
				Node importedNode = toPopulate.importNode(navigationRules.item(i), true);
				root.appendChild(importedNode);
			}
		} catch (Exception ex) {
			log.error("Failed to update navigation rules", ex);
		}
	}

	private NodeList getNavigationRules(Document document) {
		String namespace = document.getDocumentElement().getNamespaceURI();
		return document.getDocumentElement().getElementsByTagNameNS(namespace, NAVIGATION_RULE);
	}

}
