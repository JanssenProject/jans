/*
  All rights reserved -- Copyright 2015 Gluu Inc.
*/
package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parameter class for JWKS request
 *
 * @author Shoeb
 * @version 11/10/2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetJwksParams implements IParams {

    @JsonProperty(value = "op_host")
    private String op_host;

    @JsonProperty(value = "op_discovery_path")
    private String op_discovery_path;

    @JsonProperty(value = "op_configuration_endpoint")
    private String op_configuration_endpoint;

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

    public String getOpConfigurationEndpoint() {
        return op_configuration_endpoint;
    }

    public void setOpConfigurationEndpoint(String op_configuration_endpoint) {
        this.op_configuration_endpoint = op_configuration_endpoint;
    }
}
