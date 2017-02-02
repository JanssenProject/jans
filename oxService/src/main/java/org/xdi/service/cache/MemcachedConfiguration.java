package org.xdi.service.cache;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author yuriyz on 02/02/2017.
 */
@XmlRootElement
public class MemcachedConfiguration implements Serializable {

    @XmlElement(name = "servers")
    private String servers; // server1:11211 server2:11211

    @XmlElement(name = "maxOperationQueueLength")
    private int maxOperationQueueLength = 100000;

    @XmlElement(name = "bufferSize")
    private int bufferSize = 32768;

    @XmlElement(name = "putExpiration")
    private int putExpiration = 86400; // one day

    public int getPutExpiration() {
        return putExpiration;
    }

    public void setPutExpiration(int putExpiration) {
        this.putExpiration = putExpiration;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public int getMaxOperationQueueLength() {
        return maxOperationQueueLength;
    }

    public void setMaxOperationQueueLength(int maxOperationQueueLength) {
        this.maxOperationQueueLength = maxOperationQueueLength;
    }

    @Override
    public String toString() {
        return "MemcachedConfiguration{" +
                "servers='" + servers + '\'' +
                ", maxOperationQueueLength=" + maxOperationQueueLength +
                ", bufferSize=" + bufferSize +
                '}';
    }
}
