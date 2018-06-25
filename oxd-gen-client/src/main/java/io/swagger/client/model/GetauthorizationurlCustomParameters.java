package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * GetauthorizationurlCustomParameters
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class GetauthorizationurlCustomParameters {
  @SerializedName("param1")
  private String param1 = null;

  @SerializedName("param2")
  private String param2 = null;

  public GetauthorizationurlCustomParameters param1(String param1) {
    this.param1 = param1;
    return this;
  }

   /**
   * Get param1
   * @return param1
  **/
  @ApiModelProperty(required = true, value = "")
  public String getParam1() {
    return param1;
  }

  public void setParam1(String param1) {
    this.param1 = param1;
  }

  public GetauthorizationurlCustomParameters param2(String param2) {
    this.param2 = param2;
    return this;
  }

   /**
   * Get param2
   * @return param2
  **/
  @ApiModelProperty(required = true, value = "")
  public String getParam2() {
    return param2;
  }

  public void setParam2(String param2) {
    this.param2 = param2;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetauthorizationurlCustomParameters getauthorizationurlCustomParameters = (GetauthorizationurlCustomParameters) o;
    return Objects.equals(this.param1, getauthorizationurlCustomParameters.param1) &&
        Objects.equals(this.param2, getauthorizationurlCustomParameters.param2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(param1, param2);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetauthorizationurlCustomParameters {\n");
    
    sb.append("    param1: ").append(toIndentedString(param1)).append("\n");
    sb.append("    param2: ").append(toIndentedString(param2)).append("\n");
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

