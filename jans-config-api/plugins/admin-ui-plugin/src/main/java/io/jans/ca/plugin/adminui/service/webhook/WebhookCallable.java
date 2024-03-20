package io.jans.ca.plugin.adminui.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.model.webhook.WebhookEntry;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ClientFactory;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections.MapUtils;
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
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.createObjectNode();
        try {
            log.debug("Webhook processing started. Id: {}. Name: {}, URL : {}, HttpMethod: {}", webhook.getInum(), webhook.getDisplayName(), webhook.getUrl(), webhook.getHttpMethod());
            Invocation.Builder request = ClientFactory.instance().getClientBuilder(webhook.getUrl());
            //getting all headers
            webhook.getHttpHeaders().stream()
                    .filter(Objects::nonNull)
                    .forEach(header -> request.header(header.getKey(), header.getValue()));
            //Call rest endpoint
            Invocation invocation = checkHttpMethod(request);
            if (invocation == null) {
                log.error("Error in creating invocation object for rest call (Name: {}, Id: {})", webhook.getDisplayName(), webhook.getInum());
                return CommonUtils.createGenericResponse(false,
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "Error in creating invocation object for rest call (Name: " + webhook.getDisplayName() + ", Id: " + webhook.getInum() + ")");
            }
            Response response = invocation.invoke();
            log.debug("Webhook (Name: {}, Id: {}) response status code: {}", webhook.getDisplayName(), webhook.getInum(), response.getStatus());

            ((ObjectNode) jsonNode).put("webhookId", webhook.getInum());
            ((ObjectNode) jsonNode).put("webhookName", webhook.getDisplayName());
            ((ObjectNode) jsonNode).put("webhookMethod", webhook.getHttpMethod());
            if (Lists.newArrayList("POST", "PUT", "PATCH").contains(webhook.getHttpMethod())) {
                ((ObjectNode) jsonNode).put("webhookRequestBody", webhook.getHttpRequestBody().toString());
            }
            if (response.getStatus() == Response.Status.OK.getStatusCode() ||
                    response.getStatus() == Response.Status.CREATED.getStatusCode() ||
                    response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
                String responseData = response.readEntity(String.class);
                log.debug("Webhook (Name: {}, Id: {}) responseData : {}", webhook.getDisplayName(), webhook.getInum(), responseData);
                return CommonUtils.createGenericResponse(true, response.getStatus(), responseData, jsonNode);
            } else {
                String responseData = response.readEntity(String.class);
                log.error("Webhook (Name: {}, Id: {}) responseData : {}", webhook.getDisplayName(), webhook.getInum(), responseData);
                return CommonUtils.createGenericResponse(false, response.getStatus(), responseData, jsonNode);
            }
        } catch (Exception e) {
            log.error(ErrorResponse.WEBHOOK_TRIGGER_ERROR.getDescription(), e);
            ((ObjectNode) jsonNode).put("webhookId", webhook.getInum());
            ((ObjectNode) jsonNode).put("webhookName", webhook.getDisplayName());
            ((ObjectNode) jsonNode).put("webhookMethod", webhook.getHttpMethod());
            if (Lists.newArrayList("POST", "PUT", "PATCH").contains(webhook.getHttpMethod())) {
                ((ObjectNode) jsonNode).put("webhookRequestBody", webhook.getHttpRequestBody().toString());
            }
            return CommonUtils.createGenericResponse(false, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage(), jsonNode);
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
                if (MapUtils.isEmpty(webhook.getHttpRequestBody())) {
                    break;
                }
                Map<String, Object> requestBody = setRequestBody(webhook);
                if (requestBody.isEmpty()) {
                    log.error("Webhook (Name: {}, Id: {}) . Error in parsing request-body or the request-body is empty.", webhook.getDisplayName(), webhook.getInum());
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
            if (webhook.getHttpHeaders().stream().anyMatch(header -> header.getKey().equals(AppConstants.CONTENT_TYPE))) {
                Map<String, Object> reqBody = webhook.getHttpRequestBody();
                for (String key : reqBody.keySet()) {
                    body.put(key, reqBody.get(key));
                }
            }
            return body;
        } catch (Exception ex) {
            log.error("Error in parsing request-body.", ex);
            return Maps.newHashMap();
        }
    }
}
