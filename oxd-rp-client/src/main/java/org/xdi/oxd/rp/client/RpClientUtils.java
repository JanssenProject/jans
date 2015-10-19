package org.xdi.oxd.rp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class RpClientUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RpClientUtils.class);

    private RpClientUtils() {
    }

    public static HrefDetails parseHref(String href) {
        int codeIndex = startIndex(href, "code");
        int accessTokenIndex = startIndex(href, "access_token");
        int idTokenIndex = startIndex(href, "id_token");

        final HrefDetails hrefDetails = new HrefDetails();
        if (codeIndex != -1) {
            hrefDetails.setCode(substring(href, codeIndex));
        }
        if (accessTokenIndex != -1) {
            hrefDetails.setAccessToken(substring(href, accessTokenIndex));
        }
        if (idTokenIndex != -1) {
            hrefDetails.setIdToken(substring(href, idTokenIndex));
        }

        return hrefDetails;
    }

    private static String substring(String href, int startIndex) {
        try {
            int endIndex = href.indexOf("&", startIndex);
            final String result = endIndex != -1 ? href.substring(startIndex, endIndex) : href.substring(startIndex);
            return result.trim();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private static int startIndex(String href, String toSearch) {
        int index = href.indexOf("&" + toSearch + "=");
        if (index == -1) {
            index = href.indexOf("#" + toSearch + "=");
        }
        index = index + ("&" + toSearch + "=").length();
        return index;
    }
}
