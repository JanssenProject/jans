package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.IntrospectaccesstokenParams;
import java.io.IOException;

/**
 * IntrospectAccessTokenParams
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T15:27:32.160Z")
public class IntrospectAccessTokenParams {
  @SerializedName("command")
  private String command = null;

  @SerializedName("params")
  private IntrospectaccesstokenParams params = null;

  public IntrospectAccessTokenParams command(String command) {
    this.command = command;
    return this;
  }

   /**
   * Get command
   * @return command
  **/
  @ApiModelProperty(required = true, value = "")
  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public IntrospectAccessTokenParams params(IntrospectaccesstokenParams params) {
    this.params = params;
    return this;
  }

   /**
   * Get params
   * @return params
  **/
  @ApiModelProperty(required = true, value = "")
  public IntrospectaccesstokenParams getParams() {
    return params;
  }

  public void setParams(IntrospectaccesstokenParams params) {
    this.params = params;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntrospectAccessTokenParams introspectAccessTokenParams = (IntrospectAccessTokenParams) o;
    return Objects.equals(this.command, introspectAccessTokenParams.command) &&
        Objects.equals(this.params, introspectAccessTokenParams.params);
  }

  @Override
  public int hashCode() {
    return Objects.hash(command, params);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntrospectAccessTokenParams {\n");
    
    sb.append("    command: ").append(toIndentedString(command)).append("\n");
    sb.append("    params: ").append(toIndentedString(params)).append("\n");
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

