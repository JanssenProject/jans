package io.jans.ca.plugin.adminui.model.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import io.jans.as.model.config.adminui.KeyValuePair;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.apache.commons.collections.MapUtils;

import java.io.Serializable;
import java.util.*;
@DataEntry(sortBy = {"inum"})
@ObjectClass(value = "auiWebhooks")
public class WebhookEntry extends Entry implements Serializable {

    @AttributeName(
            ignoreDuringUpdate = true
    )
    private String inum;
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
    @JsonObject
    private transient Map<String, Object> httpRequestBody;
    @AttributeName(name = "httpRequestBody")
    private String httpRequestBodyString;
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
        this.inum = webhookEntry.getInum();
        this.displayName = webhookEntry.getDisplayName();
        this.description = webhookEntry.getDescription();
        this.url = webhookEntry.getUrl();
        this.httpRequestBody = webhookEntry.getHttpRequestBody();
        // This block of code is checking if the HTTP method of the `webhookEntry` object is one of "POST", "PUT", or
        // "PATCH". If it is, it then tries to convert the `httpRequestBody` of the `webhookEntry` object to a JSON string
        // and store it in `httpRequestBodyString`. Additionally, it attempts to convert the `httpRequestBodyString` back
        // to a map if it is a valid JSON string and store it in `httpRequestBody`. Any `JsonProcessingException` that
        // occurs during these operations is caught and ignored.
        if (Lists.newArrayList("POST", "PUT", "PATCH").contains(webhookEntry.getHttpMethod())) {
            try {
                if(!MapUtils.isEmpty(webhookEntry.getHttpRequestBody())) {
                    this.httpRequestBodyString = CommonUtils.mapToJsonString(webhookEntry.getHttpRequestBody());
                }
                if(CommonUtils.isValidJson(webhookEntry.getHttpRequestBodyString())) {
                    this.httpRequestBody = CommonUtils.jsonStringToMap(webhookEntry.getHttpRequestBodyString());
                }
            } catch (JsonProcessingException e) {
                //Ignore catching exception
            }
        }
        this.httpMethod = webhookEntry.getHttpMethod();
        this.jansEnabled = webhookEntry.isJansEnabled();
        this.httpHeaders = webhookEntry.getHttpHeaders();
        this.auiFeatureIds = webhookEntry.getAuiFeatureIds();
    }

    public String getHttpRequestBodyString() {
        return httpRequestBodyString;
    }

    /**
     * The function sets the HTTP request body string and converts it to a map if it is a valid JSON string.
     *
     * @param httpRequestBodyString The `setHttpRequestBodyString` method takes a string `httpRequestBodyString` as a
     * parameter. This method sets the `httpRequestBodyString` field of the class to the provided string. It also attempts
     * to convert the string to a map if it is a valid JSON string using the `CommonUtils
     */
    public void setHttpRequestBodyString(String httpRequestBodyString) {
        this.httpRequestBodyString = httpRequestBodyString;
        try {
            if(CommonUtils.isValidJson(httpRequestBodyString)) {
                this.httpRequestBody = CommonUtils.jsonStringToMap(httpRequestBodyString);
            }
        } catch (JsonProcessingException e) {
            //Ignore catching exception
        }
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
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

    /**
     * The function `setHttpRequestBody` sets the HTTP request body as a map and converts it to a JSON string using a
     * utility method.
     *
     * @param httpRequestBody A map containing key-value pairs representing the HTTP request body.
     */
    public void setHttpRequestBody(Map<String, Object> httpRequestBody) {
        this.httpRequestBody = httpRequestBody;
        try {
            if(!MapUtils.isEmpty(httpRequestBody)) {
                this.httpRequestBodyString = CommonUtils.mapToJsonString(httpRequestBody);
            }
        } catch (JsonProcessingException e) {
            //Ignore catching exception
        }
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
                ", inum='" + inum + '\'' +
                ", displayName='" + displayName + '\'' +
                ", url='" + url + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", jansEnabled=" + jansEnabled +
                ", httpHeaders='" + httpHeaders + '\'' +
                '}';
    }
}
