package org.xdi.oxd.sample.rs;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/06/2016
 */

public class Configuration implements Serializable {

    @JsonProperty(value = "host")
       private String host;
    @JsonProperty(value = "port")
    private int port;
    @JsonProperty(value = "op_host")
       private String opHost;
    @JsonProperty(value = "redirect_uri")
       private String redirectUri;

    public Configuration() {
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getOpHost() {
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
