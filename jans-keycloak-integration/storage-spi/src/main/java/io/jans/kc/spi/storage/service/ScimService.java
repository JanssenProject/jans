package io.jans.kc.spi.storage.service;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.kc.spi.storage.util.JansUtil;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.user.UserResource;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.jboss.logging.Logger;

import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.util.JsonSerialization;


public class ScimService {

    private static Logger log = Logger.getLogger(ScimService.class);

    private JansUtil jansUtil;

    public ScimService(JansUtil jansUtil) {
        this.jansUtil = jansUtil;
    }

    private String getScimUserEndpoint() {
        String scimUserEndpoint = jansUtil.getScimUserEndpoint();
        log.debugv("ScimService::getScimUserEndpoint() - scimUserEndpoint:{0}", scimUserEndpoint);
        return scimUserEndpoint;
    }

    private String getScimUserSearchEndpoint() {
        String scimUserSearchEndpoint = jansUtil.getScimUserSearchEndpoint();
        log.debugv("ScimService::getScimUserSearchEndpoint() - scimUserSearchEndpoint:{0}", scimUserSearchEndpoint);
        return scimUserSearchEndpoint;
    }

    private String requestAccessToken() {
        log.debug("ScimService::requestAccessToken()");
        String token = null;

        try {
            token = jansUtil.requestScimAccessToken();
            log.debugv("ScimService::requestAccessToken() -  token:{}", token);
        } catch (Exception ex) {
            log.errorv(ex,"ScimService::requestAccessToken() - Error while generating access token for SCIM");
            throw new RuntimeException(
                    "ScimService::requestAccessToken() - Error while generating access token for SCIM endpoint",ex);
        }
        return token;
    }

    public UserResource getUserById(String inum) {
        log.infov(" ScimService::getUserById() - inum:{0}", inum);
        try {
            return getData(getScimUserEndpoint() + "/" + inum, this.requestAccessToken());
        } catch (Exception ex) {
            log.errorv(ex,
                    "ScimService::getUserById() - Error fetching user based on inum:{0} from external service",
                    inum);
        }
        return null;
    }

    public UserResource getUserByName(String username) {
        log.infov("ScimService::getUserByName() - username:{0}", username);
        try {

            String filter = "userName eq \"" + username + "\"";
            return postData(this.getScimUserSearchEndpoint(), this.requestAccessToken(), filter);
        } catch (Exception ex) {
            log.errorv(ex,
                    "ScimService::getUserByName() - Error fetching user based on username:{0} from external service",
                    username);
        }
        return null;
    }

    public UserResource getUserByEmail(String email) {
        log.debugv(" ScimService::getUserByEmail() - email:{}", email);
        try {

            String filter = "emails[value eq \"" + email + "\"]";
            log.debugv(" ScimService::getUserByEmail() - filter:{}", filter);
            return postData(this.getScimUserSearchEndpoint(), this.requestAccessToken(), filter);
        } catch (Exception ex) {
            log.errorv(ex,
                    " ScimService::getUserByEmail() - Error fetching user based on email:{0}",
                    email);

        }
        return null;
    }

    public UserResource postData(String uri, String accessToken, String filter) {
        UserResource user = null;
        log.debugv("ScimService::postData() - uri:{0}, accessToken:{1}, filter:{2}", uri, accessToken, filter);
        try {
            HttpClient client = HttpClientBuilder.create().build();

            SearchRequest searchRequest = createSearchRequest(filter);
            log.debugv("ScimService::postData() - client:{0}, searchRequest:{1}, accessToken:{2}", client, searchRequest.toString(),
                    accessToken);

            JsonNode jsonNode = SimpleHttp.doPost(uri, client).auth(accessToken).json(searchRequest).asJson();

            log.debugv("\n\n  ScimService::postData() - jsonNode:{0}", jsonNode);

            user = getUserResourceFromList(jsonNode);

            log.debugv("ScimService::postData() - user:{0}", user);

        } catch (Exception ex) {
            log.errorv(ex,"ScimService::postData() - Error while fetching data");
        }
        return user;
    }

    public UserResource getData(String uri, String accessToken) {
        UserResource user = null;
        log.debugv("ScimService::getData() - uri:{0}, accessToken:{1}", uri, accessToken);
        try {
            HttpClient client = HttpClientBuilder.create().build();

            JsonNode jsonNode = SimpleHttp.doGet(uri, client).auth(accessToken).asJson();

            log.debugv("\n\n  ScimService::getData() - jsonNode:{0}", jsonNode);

            user = getUserResource(jsonNode);

            log.debugv("ScimService::getData() - user:{}", user);

        } catch (Exception ex) {
            log.errorv(ex,"\n\n ScimService::getData() - Error while fetching data");
        }
        return user;
    }

    private SearchRequest createSearchRequest(String filter) {
        log.debugv("ScimService::createSearchRequest() - createSearchRequest() - filter:{0}", filter);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setFilter(filter);

        log.debugv(" ScimService::createSearchRequest() - searchRequest:{0}", searchRequest);

        return searchRequest;
    }

    private UserResource getUserResourceFromList(JsonNode jsonNode) {
        log.debugv(" \n\n ScimService::getUserResourceFromList() - jsonNode:{0}", jsonNode);

        UserResource user = null;
        try {
            if (jsonNode != null) {
                if (jsonNode.get("Resources") != null) {
                    JsonNode value = jsonNode.get("Resources").get(0);
                    log.debugv("*** ScimService::getUserResourceFromList() - value:{0}, value.getClass():{1}", value,
                            value.getClass());
                    user = JsonSerialization.readValue(JsonSerialization.writeValueAsBytes(value), UserResource.class);
                    log.debugv(" ScimService::getUserResourceFromList() - user:{0}, user.getClass():{1}", user,
                            user.getClass());
                }
            }
        } catch (Exception ex) {
            log.errorv(ex,"\n\n ScimService::getUserResourceFromList() - Error while fetching data");
        }
        return user;
    }

    private UserResource getUserResource(JsonNode jsonNode) {
        log.debugv("ScimService::getUserResource() - jsonNode:{0}", jsonNode);

        UserResource user = null;
        try {
            if (jsonNode != null) {
                user = JsonSerialization.readValue(JsonSerialization.writeValueAsBytes(jsonNode), UserResource.class);
                log.debugv(" ScimService::getUserResource() - user:{0}, user.getClass():{1}", user, user.getClass());
            }
        } catch (Exception ex) {
            log.errorv(ex,"\n\n ScimService::getUserResource() - Error while fetching data is ex:{}");
        }
        return user;
    }

}
