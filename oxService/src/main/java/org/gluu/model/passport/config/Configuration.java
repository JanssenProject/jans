package org.gluu.model.passport.config;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.gluu.model.passport.config.logging.LoggingConfig;

/**
 * Created by jgomer on 2019-02-21.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private String serverURI;
    private int serverWebPort;
    private String postProfileEndpoint;
    private String spTLSCert;
    private String spTLSKey;
    private LoggingConfig logging;

    public String getServerURI() {
        return serverURI;
    }

    public void setServerURI(String serverURI) {
        this.serverURI = serverURI;
    }

    public int getServerWebPort() {
        return serverWebPort;
    }

    public void setServerWebPort(int serverWebPort) {
        this.serverWebPort = serverWebPort;
    }

    public String getPostProfileEndpoint() {
        return postProfileEndpoint;
    }

    public void setPostProfileEndpoint(String postProfileEndpoint) {
        this.postProfileEndpoint = postProfileEndpoint;
    }

    public String getSpTLSCert() {
        return spTLSCert;
    }

    public void setSpTLSCert(String spTLSCert) {
        this.spTLSCert = spTLSCert;
    }

    public String getSpTLSKey() {
        return spTLSKey;
    }

    public void setSpTLSKey(String spTLSKey) {
        this.spTLSKey = spTLSKey;
    }

    public LoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(LoggingConfig logging) {
        this.logging = logging;
    }

}
