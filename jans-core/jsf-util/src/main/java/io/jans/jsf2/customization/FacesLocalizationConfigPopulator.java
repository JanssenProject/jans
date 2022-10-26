/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.customization;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.faces.application.ApplicationConfigurationPopulator;

import io.jans.util.StringHelper;
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
 * @author Yuriy Movchan
 * @version 06/11/2020
 */
public abstract class FacesLocalizationConfigPopulator extends ApplicationConfigurationPopulator {

	private static final String DEFAULT_LANGUAGE_PATH = "/classes/";

	private Logger log = LoggerFactory.getLogger(FacesLocalizationConfigPopulator.class);

	@Override
	public void populateApplicationConfiguration(Document toPopulate) {
		populateLocalizations(toPopulate);
	}

	private void populateLocalizations(Document toPopulate) {
		log.debug("Starting localization configuration populator");

		if (Utils.isCustomLocalizationDirExists()) {
			String customLocalizationPath = Utils.getCustomLocalizationPath();
			log.debug("Adding localizations from custom dir folder: {}", customLocalizationPath);
			try {
				findAndAddLocalizations(toPopulate, customLocalizationPath);
			} catch (Exception ex) {
				FacesLogger.CONFIG.getLogger().log(Level.SEVERE, "Can't add localizations from custom dir");
			}
		}

		try {
			log.debug("Adding localizations from application resurces");
			URL[] ulrs = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
			for (URL url : ulrs) {
				if (url.getFile().endsWith(DEFAULT_LANGUAGE_PATH)) {
					int count = findAndAddLocalizations(toPopulate, url.getFile());
					if (count > 0) {
						log.debug("Added {} application localizations from war folder: {}", count, url.getFile());
					}
				}
			}
		} catch (Exception ex) {
			log.error("Failed to populate application localizations", ex);
		}
	}

	private int findAndAddLocalizations(Document toPopulate, String path) throws Exception {
		String languageFilePattern = getLanguageFilePattern();
		RegexFileFilter regexFileFilter = new RegexFileFilter(languageFilePattern);
		Pattern regexFilePatern = Pattern.compile(languageFilePattern);

		File file = new File(path);
		Collection<File> languageFiles;
		try {
			languageFiles = FileUtils.listFiles(file, regexFileFilter, DirectoryFileFilter.DIRECTORY);
		} catch (RuntimeException e) {
			log.trace("Failed to find custom localizations");
			return 0;
		}

		if (languageFiles.size() == 0) {
			return 0;
		}
		log.debug("Found '{}' language files", languageFiles.size());

		List<String> localeNames = new ArrayList<String>();
		for (File languageFile : languageFiles) {
			Matcher matcher = regexFilePatern.matcher(languageFile.getName());

			if (matcher.matches()) {
				String localeName = matcher.group(1);

				localeNames.add(localeName);
			}
		}
		log.info("Adding languages '{}' from dir folder: {}", localeNames, path);

		updateDocument(toPopulate, localeNames);
		
		return localeNames.size();
	}

	private void updateDocument(Document toPopulate, List<String> localeNames) {
		String ns = toPopulate.getDocumentElement().getNamespaceURI();
		Element rootElement = toPopulate.getDocumentElement();

		// Add application
		Node applicationNode = getChildOrCreate(toPopulate, ns, rootElement, "application");

		// Add locale-config
		Node localeConfigNode = getChildOrCreate(toPopulate, ns, applicationNode, "locale-config");

		for (String localeName : localeNames) {
			// Add supported-locale
			Element supportedLocaleElement = toPopulate.createElementNS(ns, "supported-locale");
			Node supportedLocaleNode = localeConfigNode.appendChild(supportedLocaleElement);

			supportedLocaleNode.appendChild(toPopulate.createTextNode(localeName));
		}
	}

	protected Node getChildOrCreate(Document toPopulate, String ns, Node rootElement, String childNodeName) {
		if (rootElement.hasChildNodes()) {
			NodeList nodeList = rootElement.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (StringHelper.equals(nodeList.item(i).getNodeName(), childNodeName)) {
					return nodeList.item(i);
				}
			}
		}

		Element applicationElement = toPopulate.createElementNS(ns, childNodeName);
		Node applicationNode = rootElement.appendChild(applicationElement);

		return applicationNode;
	}

	public abstract String getLanguageFilePattern();

}
