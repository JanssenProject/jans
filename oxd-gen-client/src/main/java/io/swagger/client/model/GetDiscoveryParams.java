/*
 * oxd-server
 * oxd-server
 *
 * OpenAPI spec version: 4.0.beta
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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * GetDiscoveryParams
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-07-23T08:29:06.952Z")
public class GetDiscoveryParams {
  @SerializedName("op_host")
  private String opHost = null;

  @SerializedName("op_discovery_path")
  private String opDiscoveryPath = null;

  public GetDiscoveryParams opHost(String opHost) {
    this.opHost = opHost;
    return this;
  }

   /**
   * Get opHost
   * @return opHost
  **/
  @ApiModelProperty(example = "https://<ophostname>", required = true, value = "")
  public String getOpHost() {
    return opHost;
  }

  public void setOpHost(String opHost) {
    this.opHost = opHost;
  }

  public GetDiscoveryParams opDiscoveryPath(String opDiscoveryPath) {
    this.opDiscoveryPath = opDiscoveryPath;
    return this;
  }

   /**
   * Get opDiscoveryPath
   * @return opDiscoveryPath
  **/
  @ApiModelProperty(value = "")
  public String getOpDiscoveryPath() {
    return opDiscoveryPath;
  }

  public void setOpDiscoveryPath(String opDiscoveryPath) {
    this.opDiscoveryPath = opDiscoveryPath;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetDiscoveryParams getDiscoveryParams = (GetDiscoveryParams) o;
    return Objects.equals(this.opHost, getDiscoveryParams.opHost) &&
        Objects.equals(this.opDiscoveryPath, getDiscoveryParams.opDiscoveryPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(opHost, opDiscoveryPath);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetDiscoveryParams {\n");
    
    sb.append("    opHost: ").append(toIndentedString(opHost)).append("\n");
    sb.append("    opDiscoveryPath: ").append(toIndentedString(opDiscoveryPath)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

