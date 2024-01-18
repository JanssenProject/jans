package io.jans.ca.plugin.adminui.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.model.webhook.WebhookEntry;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ClientFactory;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

public class WebhookCallable implements Callable<GenericResponse> {
    private WebhookEntry webhook;
    Logger log;

    public WebhookCallable(WebhookEntry webhook, Logger log) {
        this.webhook = webhook;
        this.log = log;
    }

    @Override
    public GenericResponse call() throws ApplicationException {
        log.debug("Webhook processing started. Id: {}. Name: {}, URL : {}, HttpMethod: {}", webhook.getWebhookId(), webhook.getDisplayName(), webhook.getUrl(), webhook.getHttpMethod());
        Invocation.Builder request = ClientFactory.instance().getClientBuilder(webhook.getUrl());
        //getting all headers
        webhook.getHttpHeaders().stream()
                .filter(Objects::nonNull)
                .forEach(header -> request.header(header.getKey(), header.getValue()));
        //Call rest endpoint
        Invocation invocation = checkHttpMethod(request);
        if (invocation == null) {
            log.error("Error in creating invocation object for rest call (Name: {}, Id: {})", webhook.getDisplayName(), webhook.getWebhookId());
            return CommonUtils.createGenericResponse(false,
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Error in creating invocation object for rest call (Name: " + webhook.getDisplayName() + ", Id: " + webhook.getWebhookId() + ")");
        }
        Response response = invocation.invoke();
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("Webhook (Name: {}, Id: {}) response status code: {}", webhook.getDisplayName(), webhook.getWebhookId(), response.getStatus());
        JsonNode jsonNode = objectMapper.createObjectNode();
        ((ObjectNode) jsonNode).put("webhookId", webhook.getWebhookId());
        ((ObjectNode) jsonNode).put("webhookName", webhook.getDisplayName());
        if (response.getStatus() == Response.Status.OK.getStatusCode() ||
                response.getStatus() == Response.Status.CREATED.getStatusCode() ||
                response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
            String responseData = response.readEntity(String.class);
            log.debug("Webhook (Name: {}, Id: {}) responseData : {}", webhook.getDisplayName(), webhook.getWebhookId(), responseData);
            return CommonUtils.createGenericResponse(true, response.getStatus(), responseData, jsonNode);
        } else {
            String responseData = response.readEntity(String.class);
            log.error("Webhook (Name: {}, Id: {}) responseData : {}", webhook.getDisplayName(), webhook.getWebhookId(), responseData);
            return CommonUtils.createGenericResponse(false, response.getStatus(), responseData, jsonNode);
        }
    }

    private Invocation checkHttpMethod(Invocation.Builder request) {
        Invocation invocation = null;
        switch (webhook.getHttpMethod().toUpperCase()) {
            case "GET":
                invocation = request.buildGet();
                break;
            case "DELETE":
                invocation = request.buildDelete();
                break;
            case "POST":
            case "PUT":
            case "PATCH":
                if (CommonUtils.isEmptyOrNullCollection(webhook.getHttpRequestBody())) {
                    break;
                }
                Map<String, Object> requestBody = setRequestBody(webhook);
                if (requestBody.isEmpty()) {
                    log.error("Webhook (Name: {}, Id: {}) . Error in parsing request-body or the request-body is empty.", webhook.getDisplayName(), webhook.getWebhookId());
                }
                invocation = request.buildPost(Entity.entity(setRequestBody(webhook), MediaType.APPLICATION_JSON));
                break;
            default:
                break;
        }
        return invocation;
    }

    private Map<String, Object> setRequestBody(WebhookEntry webhook) {
        try {
            Map<String, Object> body = new HashMap<>();
            webhook.getHttpHeaders().stream()
                    .filter(Objects::nonNull)
                    .forEach(header -> {
                        if (header.getKey().equalsIgnoreCase(AppConstants.CONTENT_TYPE) && header.getKey().equalsIgnoreCase(AppConstants.APPLICATION_JSON)) {
                            Map<String, String> reqBody = webhook.getHttpRequestBody();
                            for (String key : reqBody.keySet()) {
                                body.put(key, reqBody.get(key));
                            }
                        }
                    });
            return body;
        } catch (Exception ex) {
            log.error("Error in parsing request-body.", ex);
            return Maps.newHashMap();
        }
    }

}
