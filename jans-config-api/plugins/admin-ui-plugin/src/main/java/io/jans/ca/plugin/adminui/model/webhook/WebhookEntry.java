package io.jans.ca.plugin.adminui.model.webhook;

import io.jans.as.model.config.adminui.KeyValuePair;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.*;

@DataEntry(sortBy = {"webhookId"})
@ObjectClass(value = "auiWebhooks")
public class WebhookEntry extends Entry implements Serializable {

    @AttributeName(name = "webhookId")
    private String webhookId;
    @NotNull
    @AttributeName(name = "displayName")
    @Pattern(
            regexp = "^[a-zA-Z0-9_\\-\\:\\/\\.]+$",
            message = "Name should contain only letters, digits and underscores"
    )
    @Size(
            min = 2,
            max = 60,
            message = "Length of the Name should be between 1 and 30"
    )
    private String displayName;
    @AttributeName(name = "description")
    private String description;
    @NotNull
    @AttributeName(name = "url")
    private String url;
    @AttributeName(name = "httpRequestBody")
    @JsonObject
    private transient Map<String, Object> httpRequestBody;
    @NotNull
    @AttributeName(name = "httpMethod")
    private String httpMethod;
    @AttributeName(name = "jansEnabled")
    private boolean jansEnabled;
    @JsonObject
    @AttributeName(name = "httpHeaders")
    private List<KeyValuePair> httpHeaders = new ArrayList<>();
    private Set<String> auiFeatureIds;

    public WebhookEntry() {
    }

    public WebhookEntry(WebhookEntry webhookEntry) {
        super();
        this.webhookId = webhookEntry.getWebhookId();
        this.displayName = webhookEntry.getDisplayName();
        this.description = webhookEntry.getDescription();
        this.url = webhookEntry.getUrl();
        this.httpRequestBody = webhookEntry.getHttpRequestBody();
        this.httpMethod = webhookEntry.getHttpMethod();
        this.jansEnabled = webhookEntry.isJansEnabled();
        this.httpHeaders = webhookEntry.getHttpHeaders();
        this.auiFeatureIds = webhookEntry.getAuiFeatureIds();
    }

    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public boolean isJansEnabled() {
        return jansEnabled;
    }

    public void setJansEnabled(boolean jansEnabled) {
        this.jansEnabled = jansEnabled;
    }

    public List<KeyValuePair> getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(List<KeyValuePair> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public void addHttpHeader(String key, String value) {
        KeyValuePair header = new KeyValuePair(key, value);
        this.httpHeaders.add(header);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getHttpRequestBody() {
        if (httpRequestBody == null) {
            httpRequestBody = new HashMap<>();
        }
        return httpRequestBody;
    }

    public void setHttpRequestBody(Map<String, Object> httpRequestBody) {
        this.httpRequestBody = httpRequestBody;
    }

    public Set<String> getAuiFeatureIds() {
        return auiFeatureIds;
    }

    public void setAuiFeatureIds(Set<String> auiFeatureIds) {
        this.auiFeatureIds = auiFeatureIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WebhookEntry that = (WebhookEntry) o;
        return displayName.equals(that.displayName) && url.equals(that.url) && httpMethod.equals(that.httpMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), displayName, url, httpMethod);
    }

    @Override
    public String toString() {
        return "WebhookEntry{" +
                ", webhookId='" + webhookId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", url='" + url + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", jansEnabled=" + jansEnabled +
                ", httpHeaders='" + httpHeaders + '\'' +
                '}';
    }
}
