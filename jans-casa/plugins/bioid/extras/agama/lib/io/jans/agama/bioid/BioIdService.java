package io.jans.agama.bioid;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.jwt.*;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.StringHelper;
import io.jans.service.CacheService;

import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.Random;

import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.entity.ContentType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;

public class BioIdService {
    private static final Logger logger = LoggerFactory.getLogger(BioIdService.class);

    private BioIdConfiguration config;

    public BioIdService() {
    }

    public BioIdService(BioIdConfiguration config) {
        this.config = config;
    }

    private String generateBcid(String username) {
        String bcid = config.getStorage() + "." + config.getPartition() + "." + username.hashCode();
        return bcid;
    }

    private HTTPResponse basicAuthentication(String endpoint, String queryString, HTTPRequest.Method method) {
        logger.info("Sending request to " + endpoint);
        String basic = config.getAppIdentifier() + ":" + config.getAppSecret();
        basic = new String(Base64.getEncoder().encode(basic.getBytes(UTF_8)), UTF_8);
        HTTPRequest request = new HTTPRequest(method, new URL(endpoint));
        request.setConnectTimeout(3000);
        request.setReadTimeout(3000);
        request.setAuthorization("Basic " + basic);
        request.appendQueryString(queryString);
        HTTPResponse r = request.send();
        return r;
    }

    public String getBWSToken(String username, String task) {
        String bcid = generateBcid(username);
        String tokenEndpoint = config.getEndpoint() + "token";
        String queryString = "id=" + config.getAppIdentifier() + "&bcid=" + bcid;
        if (task != null) {
            queryString += ("&task=" + task);
        }
        HTTPResponse r = basicAuthentication(tokenEndpoint, queryString, HTTPRequest.Method.GET);
        r.ensureStatusCode(200);
        String token = r.getContent();
        logger.info("Successfully obtained BioID token");
        SignedJWT jwt = SignedJWT.parse(token);
        return token;
    }

    public boolean isEnrolled(String username) {
        String enrollmentEndpoint = config.getEndpoint() + "isenrolled";
        String bcid = generateBcid(username);
        String queryString = "bcid=" + bcid + "&trait=" + "Face";
        HTTPResponse r = basicAuthentication(enrollmentEndpoint, queryString, HTTPRequest.Method.GET);
        switch (r.getStatusCode()) {
            case 200:
                logger.info(username + " found");
                break;
            case 404:
                logger.info(username + " not found");
                break;
            default:
                logger.info("Other error");
                break;
        }
        return r.getStatusCode() == 200;
    }

}
