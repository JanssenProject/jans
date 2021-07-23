package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.configapi.security.client.AuthClientFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;

@ApplicationScoped
public class StatisticService {

    @Inject
    Logger logger;

    @Inject
    AuthClientFactory authClientFactory;

    public JsonNode getStat(String url, String token, String month, String format) {    
        return AuthClientFactory.getStatResponse(url, token, month, format);
    }

}
