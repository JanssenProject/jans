package io.jans.ca.plugin.adminui.model.webhook;

import io.jans.orm.annotation.JsonObject;

import java.io.Serializable;
import java.util.Map;

public class ShortCode implements Serializable {
    private String webhookId;
    @JsonObject
    Map<String, Object> shortCodes;

    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public Map<String, Object> getShortCodes() {
        return shortCodes;
    }

    public void setShortCodes(Map<String, Object> shortCodes) {
        this.shortCodes = shortCodes;
    }

    @Override
    public String toString() {
        return "ShortCode{" +
                "webhookId='" + webhookId + '\'' +
                ", shortCodes=" + shortCodes +
                '}';
    }
}
