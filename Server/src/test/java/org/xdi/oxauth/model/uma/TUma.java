/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.gluu.oxauth.model.uma.PermissionTicket;
import org.gluu.oxauth.model.uma.RPTResponse;
import org.gluu.oxauth.model.uma.RptIntrospectionResponse;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxauth.model.uma.UmaPermission;
import org.gluu.oxauth.model.uma.UmaResource;
import org.gluu.oxauth.model.uma.UmaResourceResponse;
import org.gluu.oxauth.model.uma.wrapper.Token;
import org.gluu.oxauth.util.ServerUtil;

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

	public static Token newTokenByRefreshToken(URI baseUri, final String tokenPath, final Token p_oldToken,
			final String umaClientId, final String umaClientSecret) {
		final TTokenRequest r = new TTokenRequest(baseUri);
		return r.newTokenByRefreshToken(tokenPath, p_oldToken, umaClientId, umaClientSecret);
	}

	public static RPTResponse requestRpt(URI baseUri, String p_rptPath) {
		final TTokenRequest r = new TTokenRequest(baseUri);
		return r.requestRpt(p_rptPath);
	}

	public static UmaMetadata requestConfiguration(URI baseUri, final String configurationPath) {
		final TConfiguration c = new TConfiguration(baseUri);
		return c.getConfiguration(configurationPath);
	}

	public static UmaResourceResponse registerResource(URI baseUri, Token p_pat, String p_umaRegisterResourcePath,
													   UmaResource resource) {
		final TRegisterResource s = new TRegisterResource(baseUri);
		return s.registerResource(p_pat, p_umaRegisterResourcePath, resource);
	}

	public static UmaResourceResponse modifyResource(URI baseUri, Token p_pat, String p_umaRegisterResourcePath,
													 final String p_rsid, UmaResource resource) {
		final TRegisterResource s = new TRegisterResource(baseUri);
		return s.modifyResource(p_pat, p_umaRegisterResourcePath, p_rsid, resource);
	}

	public static List<String> getResourceList(URI baseUri, Token p_pat, String p_umaRegisterResourcePath) {
		final TRegisterResource s = new TRegisterResource(baseUri);
		return s.getResourceList(p_pat, p_umaRegisterResourcePath);
	}

	public static void deleteResource(URI baseUri, Token p_pat, String p_umaRegisterResourcePath, String p_id) {
		final TRegisterResource s = new TRegisterResource(baseUri);
		s.deleteResource(p_pat, p_umaRegisterResourcePath, p_id);
	}

	public static PermissionTicket registerPermission(URI baseUri, Token p_pat, UmaPermission p_request, String p_umaPermissionPath) {
		final TRegisterPermission p = new TRegisterPermission(baseUri);
		return p.registerPermission(p_pat, p_request, p_umaPermissionPath);
	}

	public static RptIntrospectionResponse requestRptStatus(URI baseUri, String p_umaRptStatusPath,	String rpt) {
		final TTokenRequest r = new TTokenRequest(baseUri);
		return r.requestRptStatus(p_umaRptStatusPath, rpt);
	}

	public static <T> T readJsonValue(String p_json, Class<T> p_clazz) {
		try {
			return ServerUtil.createJsonMapper().readValue(p_json, p_clazz);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				System.out.println("TUMA: Exception happends, try falback");
				return ServerUtil.jsonMapperWithUnwrapRoot().readValue(p_json, p_clazz);
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}
		}
	}
}
