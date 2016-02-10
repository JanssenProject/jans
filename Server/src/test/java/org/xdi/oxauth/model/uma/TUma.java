/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.util.ServerUtil;

import java.io.IOException;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class TUma {
    private TUma() {
    }

    public static Token requestPat(BaseTest p_test, final String authorizePath, final String tokenPath,
                                   final String userId, final String userSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri) {
        final TTokenRequest r = new TTokenRequest(p_test);
        return r.pat(authorizePath, tokenPath, userId, userSecret, umaClientId, umaClientSecret, umaRedirectUri);
    }

    public static Token requestAat(BaseTest p_test, final String authorizePath, final String tokenPath,
                                   final String userId, final String userSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri) {
        final TTokenRequest r = new TTokenRequest(p_test);
        return r.aat(authorizePath, tokenPath, userId, userSecret, umaClientId, umaClientSecret, umaRedirectUri);
    }

    public static Token newTokenByRefreshToken(BaseTest p_test, final String tokenPath, final Token p_oldToken, final String umaClientId, final String umaClientSecret) {
        final TTokenRequest r = new TTokenRequest(p_test);
        return r.newTokenByRefreshToken(tokenPath, p_oldToken, umaClientId, umaClientSecret);
    }

    public static RPTResponse requestRpt(BaseTest p_test, Token p_aat, String p_rptPath, String umaAmHost) {
        final TTokenRequest r = new TTokenRequest(p_test);
        return r.requestRpt(p_aat, p_rptPath, umaAmHost);
    }

    public static UmaConfiguration requestConfiguration(BaseTest p_test, final String configurationPath) {
        final TConfiguration c = new TConfiguration(p_test);
        return c.getConfiguration(configurationPath);
    }

    public static ResourceSetResponse registerResourceSet(BaseTest p_test, Token p_pat, String p_umaRegisterResourcePath, ResourceSet p_resourceSet) {
        final TRegisterResourceSet s = new TRegisterResourceSet(p_test);
        return s.registerResourceSet(p_pat, p_umaRegisterResourcePath, p_resourceSet);
    }

    public static ResourceSetResponse modifyResourceSet(BaseTest p_test, Token p_pat, String p_umaRegisterResourcePath,
                                                      final String p_rsid, ResourceSet p_resourceSet) {
        final TRegisterResourceSet s = new TRegisterResourceSet(p_test);
        return s.modifyResourceSet(p_pat, p_umaRegisterResourcePath, p_rsid, p_resourceSet);
    }

    public static List<String> getResourceSetList(BaseTest p_test, Token p_pat, String p_umaRegisterResourcePath) {
        final TRegisterResourceSet s = new TRegisterResourceSet(p_test);
        return s.getResourceSetList(p_pat, p_umaRegisterResourcePath);
    }

    public static void deleteResourceSet(BaseTest p_test, Token p_pat, String p_umaRegisterResourcePath, String p_id) {
        final TRegisterResourceSet s = new TRegisterResourceSet(p_test);
        s.deleteResourceSet(p_pat, p_umaRegisterResourcePath, p_id);
    }

    public static PermissionTicket registerPermission(BaseTest p_test, Token p_pat, String p_umaAmHost, String p_umaHost,
                                                                UmaPermission p_request, String p_umaPermissionPath) {
        final TRegisterPermission p = new TRegisterPermission(p_test);
        return p.registerPermission(p_pat, p_umaAmHost, p_umaHost, p_request, p_umaPermissionPath);
    }

    public static RptIntrospectionResponse requestRptStatus(BaseTest p_test, String p_umaRptStatusPath, String p_umaAmHost, Token p_aat, String rpt) {
        final TTokenRequest r = new TTokenRequest(p_test);
        return r.requestRptStatus(p_umaRptStatusPath, p_umaAmHost, p_aat, rpt);
    }

    public static RptAuthorizationResponse requestAuthorization(BaseTest p_test, String p_umaPermissionAuthorizationPath, String p_umaAmHost, Token p_aat, RptAuthorizationRequest p_request) {
        final TAuthorization t = new TAuthorization(p_test);
        return t.requestAuthorization(p_umaPermissionAuthorizationPath, p_umaAmHost, p_aat, p_request);
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
