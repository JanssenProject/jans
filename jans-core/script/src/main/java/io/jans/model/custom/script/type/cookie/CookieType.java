package io.jans.model.custom.script.type.cookie;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface CookieType extends BaseExternalType {

    /**
     *
     * @param cookieName cookie name
     * @param cookieHeader "Set-Cookie" cookie header value that is set in response
     * @return "Set-Cookie" cookie header value
     */
    String modifyCookieHeader(String cookieName, String cookieHeader);
}
