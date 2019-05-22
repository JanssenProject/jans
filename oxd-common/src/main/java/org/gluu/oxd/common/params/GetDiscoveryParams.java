package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetDiscoveryParams implements IParams{

    @JsonProperty(value = "op_host")
    private String op_host;

    @JsonProperty(value = "op_discovery_path")
    private String op_discovery_path;

    public GetDiscoveryParams() {
    }

    public String getOpHost() {
        return op_host;
    }

    public void setOpHost(String opHost) {
        this.op_host = opHost;
    }

    public String getOpDiscoveryPath() {
        return op_discovery_path;
    }

    public void setOpDiscoveryPath(String opDiscoveryPath) {
        this.op_discovery_path = opDiscoveryPath;
    }

    @Override
    public String toString() {
        return "GetDiscoveryParams{" +
                "op_host='" + op_host + '\'' +
                ", op_discovery_path='" + op_discovery_path + '\'' +
                '}';
    }
}
