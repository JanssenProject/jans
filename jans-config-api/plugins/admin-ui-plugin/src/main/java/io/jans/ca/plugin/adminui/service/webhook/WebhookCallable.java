package io.jans.ca.plugin.adminui.service.webhook;


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
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
        log.info("Webhook processing started. Id: {}. Name: {}, URL : {}, HttpMethod: {}", webhook.getWebhookId(), webhook.getDisplayName(), webhook.getUrl(), webhook.getHttpMethod());
        Invocation.Builder request = ClientFactory.instance().getClientBuilder(webhook.getUrl());
        //getting all headers
        webhook.getHttpHeaders().stream()
                .filter(header -> header != null)
                .forEach(header -> {
                    request.header(header.getKey(), header.getValue());
                });
        //Call rest endpoint
        Response response = checkHttpMethod(request).invoke();

        log.info("Webhook (Name: {}, Id: {}) request status code: {}", webhook.getDisplayName(), webhook.getWebhookId(), response.getStatus());
        if (response.getStatus() == Response.Status.OK.getStatusCode() ||
                response.getStatus() == Response.Status.CREATED.getStatusCode() ||
                response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
            String responseData = response.readEntity(String.class);
            log.info("Webhook (Name: {}, Id: {}) responseData", webhook.getDisplayName(), webhook.getWebhookId(), responseData);
            return CommonUtils.createGenericResponse(true, response.getStatus(), responseData);
        } else {
            String responseData = response.readEntity(String.class);
            log.error("Webhook (Name: {}, Id: {}) responseData", webhook.getDisplayName(), webhook.getWebhookId(), responseData);
            return CommonUtils.createGenericResponse(false, response.getStatus(), responseData);
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
            case "POST":
            case "PUT":
            case "PATCH":
                if (webhook.getHttpRequestBody() == null) {
                    break;
                }
                Map<String, Object> requestBody = setRequestBody(webhook);
                if (requestBody == null) {
                    log.error("Webhook (Name: {}, Id: {}) . Error in parsing request-body", webhook.getDisplayName(), webhook.getWebhookId());
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
                    .filter(header -> header != null)
                    .forEach(header -> {
                        if (header.getKey().equalsIgnoreCase(AppConstants.CONTENT_TYPE) && header.getKey().equalsIgnoreCase(AppConstants.APPLICATION_JSON)) {
                            JSONObject reqBody = new JSONObject(webhook.getHttpRequestBody());
                            Iterator reqBodyIte = reqBody.keys();
                            while (reqBodyIte.hasNext()) {
                                String key = reqBodyIte.next().toString();
                                body.put(key, reqBody.get(key));
                            }
                        }
                    });
            return body;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
