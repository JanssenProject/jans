package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/03/2015
 */

@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"format", "token"})
@XmlRootElement
public class ClaimToken implements Serializable {

    private String format;
    private String token;

    public ClaimToken() {
    }

    public ClaimToken(String format, String token) {
        this.format = format;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty(value = "format")
    @XmlElement(name = "format")
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }


//        public Map<String, List<String>> getFormat() {
//            return format;
//        }

}
