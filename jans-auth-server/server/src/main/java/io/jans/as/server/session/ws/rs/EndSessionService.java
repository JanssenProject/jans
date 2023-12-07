package io.jans.as.server.session.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.URLPatternList;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Named
public class EndSessionService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    public boolean isUrlWhiteListed(String url) {
        final boolean result = new URLPatternList(appConfiguration.getClientWhiteList()).isUrlListed(url);
        log.trace("White listed result: {}, url: {}", result, url);
        return result;
    }
}
