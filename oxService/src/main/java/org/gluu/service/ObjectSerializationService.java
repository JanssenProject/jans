/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;

/**
 * Service class to serialize/deserialize object to file
 *
 * @author Yuriy Movchan Date: 01/27/2014
 */
@ApplicationScoped
@Named
public class ObjectSerializationService {

	@Inject
	private Logger log;

	public boolean saveObject(String path, Serializable obj, boolean append) {
		File file = new File(path);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file, append);
		} catch (FileNotFoundException ex) {
			log.error("Faield to serialize to file: '{}'. Error: ", path, ex);

			return false;
		}

		;
		try (GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(fos))) {
			SerializationUtils.serialize(obj, gos);
			gos.flush();
		} catch (IOException ex) {
			log.error("Faield to serialize to file: '{}'. Error: ", path, ex);
			return false;
		}

		return true;
	}

	public boolean saveObject(String path, Serializable obj) {
		return saveObject(path, obj, false);
	}

	public Object loadObject(String path) {
		File file = new File(path);
		if (!file.exists()) {
			log.trace("File '{}' is not exist", path);
			return null;
		}

		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException ex) {
			log.error("Faield to deserialize from file: '{}'. Error: ", path, ex);

			return null;
		}

		Object obj = null;
		try (GZIPInputStream gis = new GZIPInputStream(new BufferedInputStream(fis))) {
			;
			obj = SerializationUtils.deserialize(gis);
		} catch (IOException ex) {
			log.error("Faield to deserialize from file: '{}'. Error: ", path, ex);
			return null;
		}
		return obj;
	}

	public void cleanup(String path) {
		File file = new File(path);
		FileUtils.deleteQuietly(file);
	}

}
