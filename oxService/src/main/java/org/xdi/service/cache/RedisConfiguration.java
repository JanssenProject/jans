package org.xdi.service.cache;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author yuriyz on 02/23/2017.
 */
@XmlRootElement
public class RedisConfiguration implements Serializable {

    @XmlElement(name = "host")
    private String host = "localhost";

    @XmlElement(name = "port")
    private int port = 6379;

    @XmlElement(name = "defaultPutExpiration")
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
