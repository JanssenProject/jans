package io.jans.ca.plugin.adminui.model.webhook;

import io.jans.orm.annotation.JsonObject;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;

public class ShortCodeRequest implements Serializable {
    @NotNull
    private String webhookId;
    @JsonObject
    transient  Map<String, Object> shortcodeValueMap;

    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public Map<String, Object> getShortcodeValueMap() {
        return shortcodeValueMap;
    }

    public void setShortcodeValueMap(Map<String, Object> shortcodeValueMap) {
        this.shortcodeValueMap = shortcodeValueMap;
    }

    @Override
    public String toString() {
        return "ShortCodeRequest{" +
                "webhookId='" + webhookId + '\'' +
                ", shortcodeValueMap=" + shortcodeValueMap +
                '}';
    }
}
