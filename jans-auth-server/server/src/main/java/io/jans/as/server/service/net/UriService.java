package io.jans.as.server.service.net;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.URLPatternList;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class UriService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    public boolean isExternalUriWhitelisted(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }

        final List<String> externalUriWhiteList = appConfiguration.getExternalUriWhiteList();
        if (externalUriWhiteList == null || externalUriWhiteList.isEmpty()) {
            return true;
        }

        return new URLPatternList(externalUriWhiteList).isUrlListed(uri);
    }

    /**
     * Unlike {@link #isExternalUriWhitelisted(String)}, an empty/unconfigured externalUriWhiteList
     * means "no exception granted" here, not "no restriction" - for callers that use a whitelist
     * match as an opt-in bypass of an otherwise fail-closed security check (e.g. private-address
     * validation for sector_identifier_uri/client_id), where defaulting to open would be a
     * security regression rather than a permissive convenience.
     */
    public boolean isExplicitlyWhitelisted(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }

        final List<String> externalUriWhiteList = appConfiguration.getExternalUriWhiteList();
        if (externalUriWhiteList == null || externalUriWhiteList.isEmpty()) {
            return false;
        }

        return new URLPatternList(externalUriWhiteList).isUrlListed(uri);
    }

    public JSONObject loadJson(String uri) {
        if (!isExternalUriWhitelisted(uri)) {
            log.debug("Unable to call external uri: {}, externalUriWhiteList: {}", uri, appConfiguration.getExternalUriWhiteList());
            return null;
        }
        return JwtUtil.getJSONWebKeys(uri);
    }
}
