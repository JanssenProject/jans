/*
 * oxd-server
 * oxd-server
 *
 * OpenAPI spec version: 4.2
 * Contact: yuriyz@gluu.org
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * GetIssuerParams
 */


public class GetIssuerParams {
  @SerializedName("op_configuration_endpoint")
  private String opConfigurationEndpoint = null;

  @SerializedName("op_host")
  private String opHost = null;

  @SerializedName("op_discovery_path")
  private String opDiscoveryPath = null;

  @SerializedName("resource")
  private String resource = null;

  public GetIssuerParams opConfigurationEndpoint(String opConfigurationEndpoint) {
    this.opConfigurationEndpoint = opConfigurationEndpoint;
    return this;
  }

   /**
   * The openid configuration endpoint URL. If missing, then &#x60;op_host&#x60; must be defined.
   * @return opConfigurationEndpoint
  **/
  @Schema(example = "https://op.example.com/acme/.well-known/openid-configuration", required = true, description = "The openid configuration endpoint URL. If missing, then `op_host` must be defined.")
  public String getOpConfigurationEndpoint() {
    return opConfigurationEndpoint;
  }

  public void setOpConfigurationEndpoint(String opConfigurationEndpoint) {
    this.opConfigurationEndpoint = opConfigurationEndpoint;
  }

  public GetIssuerParams opHost(String opHost) {
    this.opHost = opHost;
    return this;
  }

   /**
   * Deprecated in favor of &#x60;op_configuration_endpoint&#x60;. It will be removed in future version(s). Provide the URL of OpenID Provider (OP) in this field. If missing, then &#x60;op_configuration_endpoint&#x60; must be defined.
   * @return opHost
  **/
  @Schema(example = "https://<ophostname>", description = "Deprecated in favor of `op_configuration_endpoint`. It will be removed in future version(s). Provide the URL of OpenID Provider (OP) in this field. If missing, then `op_configuration_endpoint` must be defined.")
  public String getOpHost() {
    return opHost;
  }

  public void setOpHost(String opHost) {
    this.opHost = opHost;
  }

  public GetIssuerParams opDiscoveryPath(String opDiscoveryPath) {
    this.opDiscoveryPath = opDiscoveryPath;
    return this;
  }

   /**
   * Deprecated in favor of &#x60;op_configuration_endpoint&#x60;. It will be removed in future version(s). Provide path to the OpenID Connect Provider&#x27;s discovery document in this field. For example, if it is &#x27;https://example.com/.well-known/openid-configuration&#x27; then the path is blank. But if it is &#x27;https://example.com/oxauth/.well-known/openid-configuration&#x27; then the path is &#x27;/oxauth&#x27;
   * @return opDiscoveryPath
  **/
  @Schema(example = "/oxauth", description = "Deprecated in favor of `op_configuration_endpoint`. It will be removed in future version(s). Provide path to the OpenID Connect Provider's discovery document in this field. For example, if it is 'https://example.com/.well-known/openid-configuration' then the path is blank. But if it is 'https://example.com/oxauth/.well-known/openid-configuration' then the path is '/oxauth'")
  public String getOpDiscoveryPath() {
    return opDiscoveryPath;
  }

  public void setOpDiscoveryPath(String opDiscoveryPath) {
    this.opDiscoveryPath = opDiscoveryPath;
  }

  public GetIssuerParams resource(String resource) {
    this.resource = resource;
    return this;
  }

   /**
   * Identifier for the target End-User that is the subject of the discovery request.
   * @return resource
  **/
  @Schema(example = "admin@jenkins-ldap.gluu.org", required = true, description = "Identifier for the target End-User that is the subject of the discovery request.")
  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetIssuerParams getIssuerParams = (GetIssuerParams) o;
    return Objects.equals(this.opConfigurationEndpoint, getIssuerParams.opConfigurationEndpoint) &&
        Objects.equals(this.opHost, getIssuerParams.opHost) &&
        Objects.equals(this.opDiscoveryPath, getIssuerParams.opDiscoveryPath) &&
        Objects.equals(this.resource, getIssuerParams.resource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(opConfigurationEndpoint, opHost, opDiscoveryPath, resource);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetIssuerParams {\n");
    
    sb.append("    opConfigurationEndpoint: ").append(toIndentedString(opConfigurationEndpoint)).append("\n");
    sb.append("    opHost: ").append(toIndentedString(opHost)).append("\n");
    sb.append("    opDiscoveryPath: ").append(toIndentedString(opDiscoveryPath)).append("\n");
    sb.append("    resource: ").append(toIndentedString(resource)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
