package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.configapi.security.client.AuthClientFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class AuthService {

    @Inject
    Logger logger;

    @Inject
    AuthClientFactory authClientFactory;

    public JsonNode getStat(String url, String token, String month, String startMonth, String endMonth, String format) {
        return AuthClientFactory.getStatResponse(url, token, month, startMonth, endMonth, format);
    }

    public JsonNode getHealthCheckResponse(String url) {
        return AuthClientFactory.getHealthCheckResponse(url);
    }

}
