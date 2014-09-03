package org.xdi.oxauth.model.uma;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/04/2013
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"status"})
@XmlRootElement
public class AuthorizationResponse {

    private String m_status;

    public AuthorizationResponse() {
    }

    public AuthorizationResponse(String p_status) {
        m_status = p_status;
    }

    @JsonProperty(value = "status")
    @XmlElement(name = "status")
    public String getStatus() {
        return m_status;
    }

    public void setStatus(String p_status) {
        m_status = p_status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AuthorizationResponse");
        sb.append("{m_status='").append(m_status).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

