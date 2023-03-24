package io.jans.as.server.service.net;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.URLPatternList;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.tika.utils.StringUtils;
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

    public boolean canCall(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }

        final List<String> externalUriWhiteList = appConfiguration.getExternalUriWhiteList();
        if (externalUriWhiteList == null || externalUriWhiteList.isEmpty()) {
            return true;
        }

        return new URLPatternList(externalUriWhiteList).isUrlListed(uri);
    }

    public JSONObject loadJson(String uri) {
        if (!canCall(uri)) {
            log.debug("Unable to call external uri: {}, externalUriWhiteList: {}", uri, appConfiguration.getExternalUriWhiteList());
            return null;
        }
        return JwtUtil.getJSONWebKeys(uri);
    }
}
