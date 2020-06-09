package org.gluu.oxd.common.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.gluu.oxauth.model.discovery.WebFingerLink;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetIssuerResponse implements IOpResponse {

    @JsonProperty(value = "subject")
    private String subject;
    @JsonProperty(value = "links")
    private List<WebFingerLink> links;

    public GetIssuerResponse() {
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<WebFingerLink> getLinks() {
        return links;
    }

    public void setLinks(List<WebFingerLink> links) {
        this.links = links;
    }

    @Override
    public String toString() {
        return "WebfingerResponse{" +
                "subject='" + subject + '\'' +
                ", links=" + links +
                '}';
    }
}
