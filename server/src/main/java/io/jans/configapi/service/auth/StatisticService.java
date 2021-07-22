package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.configapi.security.client.AuthClientFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
@ApplicationScoped
public class StatisticService {

    @Inject
    Logger logger;

    @Inject
    AuthClientFactory authClientFactory;

    //public JsonNode getUserStat(String url, String token, String month, String format) {
    public JsonNode getUserStat(String url, String token, String month, String format) {
        logger.info("\n\n StatisticService:::getUserStat() - url = " + url + " , token = " + token + " , month = "
                + month + " , format = " + format);

        return AuthClientFactory.getStatResponse(url, token, month, format);

    }

}
