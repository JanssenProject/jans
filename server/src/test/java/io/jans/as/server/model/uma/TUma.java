/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.uma;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.RPTResponse;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaResource;
import io.jans.as.model.uma.UmaResourceResponse;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.server.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class TUma {
	private TUma() {
	}

	public static Token requestPat(URI baseUri, final String authorizePath, final String tokenPath, final String userId,
			final String userSecret, final String umaClientId, final String umaClientSecret,
			final String umaRedirectUri) {
		final TTokenRequest r = new TTokenRequest(baseUri);
		return r.pat(authorizePath, tokenPath, userId, userSecret, umaClientId, umaClientSecret, umaRedirectUri);
	}

	public static Token newTokenByRefreshToken(URI baseUri, final String tokenPath, final Token oldToken,
			final String umaClientId, final String umaClientSecret) {
		final TTokenRequest r = new TTokenRequest(baseUri);
		return r.newTokenByRefreshToken(tokenPath, oldToken, umaClientId, umaClientSecret);
	}

	public static RPTResponse requestRpt(URI baseUri, String rptPath) {
		final TTokenRequest r = new TTokenRequest(baseUri);
		return r.requestRpt(rptPath);
	}

	public static UmaMetadata requestConfiguration(URI baseUri, final String configurationPath) {
		final TConfiguration c = new TConfiguration(baseUri);
		return c.getConfiguration(configurationPath);
	}

	public static UmaResourceResponse registerResource(URI baseUri, Token pat, String umaRegisterResourcePath,
													   UmaResource resource) {
		final TRegisterResource s = new TRegisterResource(baseUri);
		return s.registerResource(pat, umaRegisterResourcePath, resource);
	}

	public static UmaResourceResponse modifyResource(URI baseUri, Token pat, String umaRegisterResourcePath,
													 final String rsid, UmaResource resource) {
		final TRegisterResource s = new TRegisterResource(baseUri);
		return s.modifyResource(pat, umaRegisterResourcePath, rsid, resource);
	}

	public static List<String> getResourceList(URI baseUri, Token pat, String umaRegisterResourcePath) {
		final TRegisterResource s = new TRegisterResource(baseUri);
		return s.getResourceList(pat, umaRegisterResourcePath);
	}

	public static void deleteResource(URI baseUri, Token pat, String umaRegisterResourcePath, String id) {
		final TRegisterResource s = new TRegisterResource(baseUri);
		s.deleteResource(pat, umaRegisterResourcePath, id);
	}

	public static PermissionTicket registerPermission(URI baseUri, Token pat, UmaPermission request, String umaPermissionPath) {
		final TRegisterPermission p = new TRegisterPermission(baseUri);
		return p.registerPermission(pat, request, umaPermissionPath);
	}

	public static RptIntrospectionResponse requestRptStatus(URI baseUri, String umaRptStatusPath,	String rpt) {
		final TTokenRequest r = new TTokenRequest(baseUri);
		return r.requestRptStatus(umaRptStatusPath, rpt);
	}

	public static <T> T readJsonValue(String json, Class<T> clazz) {
		try {
			return ServerUtil.createJsonMapper().readValue(json, clazz);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				System.out.println("TUMA: Exception happends, try falback");
				return ServerUtil.jsonMapperWithUnwrapRoot().readValue(json, clazz);
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}
		}
	}
}
