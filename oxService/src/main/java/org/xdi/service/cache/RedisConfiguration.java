package org.xdi.service.cache;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author yuriyz on 02/23/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedisConfiguration implements Serializable {

	private static final long serialVersionUID = 5513197227832695470L;

	private String host = "localhost";

    private int port = 6379;

    private int defaultPutExpiration = 60; // in seconds

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getDefaultPutExpiration() {
        return defaultPutExpiration;
    }

    public void setDefaultPutExpiration(int defaultPutExpiration) {
        this.defaultPutExpiration = defaultPutExpiration;
    }

    @Override
    public String toString() {
        return "RedisConfiguration{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", defaultPutExpiration=" + defaultPutExpiration +
                '}';
    }
}
