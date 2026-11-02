package io.jans.as.common.util;

import com.google.common.base.Strings;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.URLPatternList;
import io.jans.service.cdi.util.CdiUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

    private CommonUtils() {
    }

    public static JSONObject getJwks(Client client) {
        if (!Strings.isNullOrEmpty(client.getJwks())) {
            return new JSONObject(client.getJwks());
        }

        final String jwksUri = client.getJwksUri();
        if (Strings.isNullOrEmpty(jwksUri)) {
            return null;
        }
        if (!isExternalUriWhitelisted(jwksUri, getExternalUriWhiteList())) {
            log.debug("Unable to call client jwks_uri: {}, it is not allowed by externalUriWhiteList", jwksUri);
            return null;
        }
        return JwtUtil.getJSONWebKeys(jwksUri);
    }

    static boolean isExternalUriWhitelisted(String uri, List<String> externalUriWhiteList) {
        if (externalUriWhiteList == null || externalUriWhiteList.isEmpty()) {
            return true;
        }
        try {
            return new URLPatternList(externalUriWhiteList).isUrlListed(uri);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static List<String> getExternalUriWhiteList() {
        try {
            return CdiUtil.bean(AppConfiguration.class).getExternalUriWhiteList();
        } catch (RuntimeException e) {
            // outside of CDI container (e.g. standalone usage) there is no configured whitelist
            return null;
        }
    }
}
