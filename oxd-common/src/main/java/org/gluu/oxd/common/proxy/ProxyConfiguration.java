package org.gluu.oxd.common.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class ProxyConfiguration {
    private String host;
    private Integer port;
    private String protocol;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return "ProxyConfiguration{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", protocol='" + protocol + '\'' +
                '}';
    }
}
