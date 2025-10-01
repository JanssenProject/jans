package io.jans.as.server.service.external;

import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.cookie.CookieType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ExternalCookieService extends ExternalScriptService {

    public ExternalCookieService() {
        super(CustomScriptType.COOKIE);
    }

    public String modifyCookieHeader(String cookieName, String cookieHeader) {
        final CustomScriptConfiguration script = getDefaultExternalCustomScript();
        if (script == null) {
            log.trace("No cookie script set.");
            return cookieHeader;
        }

        try {
            log.trace("Executing python 'modifyCookieHeader' method, script name: {}, cookieName: {}, cookieHeader: {}", script.getName(), cookieName, cookieHeader);

            CookieType cookieType = (CookieType) script.getExternalType();
            final String headerFromScript = cookieType.modifyCookieHeader(cookieName, cookieHeader);
            log.trace("Finished 'modifyCookieHeader' method, script name: {}, cookieName: {}, cookieHeader: {}, headerFromScript: {}", script.getName(), cookieName, cookieHeader, headerFromScript);

            return headerFromScript;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(script.getCustomScript(), e);
        }

        // fallback to original cookie header
        return cookieHeader;
    }
}
