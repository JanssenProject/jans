/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.dev;

import org.gluu.oxauth.model.config.BaseDnConfiguration;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.error.ErrorMessage;
import org.gluu.oxauth.model.error.ErrorMessages;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;
import org.gluu.oxauth.util.ServerUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9 February 12, 2015
 */

public class ConfSerialization {

	public static final String CONFIG_FOLDER = "U:\\own\\project\\oxAuth\\Server\\src\\main\\webapp\\WEB-INF\\";

	public static <T> T loadXml(String p_fileName, Class p_clazz) {
		final URL url = ConfSerialization.class.getResource(p_fileName);
		try {
			System.out.println("Loading configuration file: " + url);

			JAXBContext jc = JAXBContext.newInstance(p_clazz);
			Unmarshaller u = jc.createUnmarshaller();
			return (T) u.unmarshal(url);
		} catch (JAXBException e) {
			System.out.println("Failed to get the configuration file: " + url);
			e.printStackTrace();
		}
		return null;
	}

	public static <T> T loadJson(File p_file, Class p_clazz) {
		try {
			return (T) ServerUtil.createJsonMapper().readValue(p_file, p_clazz);
		} catch (Exception e) {
			System.out.println("Failed to load json from file: " + p_file.getPath());
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void errorXmlDeserializer() throws IOException {
		final ErrorMessages objFromXml = loadXml("oxauth-errors.xml", ConfSerialization.class);
		Assert.assertNotNull(objFromXml);

		final String jsonStr = ServerUtil.createJsonMapper().writeValueAsString(objFromXml);
		System.out.println(jsonStr);
	}

	@Test
	public void errorJsonDeserializer() throws IOException {
		final ErrorMessages object = loadJson(new File(CONFIG_FOLDER + "oxauth-errors.json"), ErrorMessages.class);
		Assert.assertTrue(object != null && notEmpty(object.getAuthorize()) && notEmpty(object.getUma())
				&& notEmpty(object.getUserInfo()) && notEmpty(object.getClientInfo()) && notEmpty(object.getToken())
				&& notEmpty(object.getEndSession()));
	}

	@Test
	public void webKeysJsonDeserializer() throws IOException {
		final JSONWebKeySet obj = loadJson(new File(CONFIG_FOLDER + "oxauth-web-keys.json"), JSONWebKeySet.class);
		Assert.assertTrue(obj != null && obj.getKeys() != null && !obj.getKeys().isEmpty());
	}

	private static boolean notEmpty(List<ErrorMessage> p_list) {
		return p_list != null && !p_list.isEmpty();
	}

	@Test
	public void claims() throws IOException {

		final BaseDnConfiguration baseDn = new BaseDnConfiguration();
		baseDn.setConfiguration("ou=configuration,o=gluu");
		baseDn.setPeople("ou=people,o=gluu");
		baseDn.setClients("ou=clients,o=gluu");
		baseDn.setScopes("ou=scopes,o=gluu");
		baseDn.setAttributes("ou=attributes,o=gluu");

		final StaticConfiguration c = new StaticConfiguration();
		c.setBaseDn(baseDn);

		final String jsonStr = ServerUtil.createJsonMapper().writeValueAsString(c);
		System.out.println(jsonStr);
	}
}