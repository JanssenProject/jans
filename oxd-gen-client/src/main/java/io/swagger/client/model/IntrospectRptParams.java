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
 * IntrospectRptParams
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T15:27:32.160Z")
public class IntrospectRptParams {
  @SerializedName("oxd_id")
  private String oxdId = null;

  @SerializedName("rpt")
  private String rpt = null;

  public IntrospectRptParams oxdId(String oxdId) {
    this.oxdId = oxdId;
    return this;
  }

   /**
   * Get oxdId
   * @return oxdId
  **/
  @ApiModelProperty(example = "bcad760f-91ba-46e1-a020-05e4281d91b6", required = true, value = "")
  public String getOxdId() {
    return oxdId;
  }

  public void setOxdId(String oxdId) {
    this.oxdId = oxdId;
  }

  public IntrospectRptParams rpt(String rpt) {
    this.rpt = rpt;
    return this;
  }

   /**
   * Get rpt
   * @return rpt
  **/
  @ApiModelProperty(required = true, value = "")
  public String getRpt() {
    return rpt;
  }

  public void setRpt(String rpt) {
    this.rpt = rpt;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntrospectRptParams introspectRptParams = (IntrospectRptParams) o;
    return Objects.equals(this.oxdId, introspectRptParams.oxdId) &&
        Objects.equals(this.rpt, introspectRptParams.rpt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oxdId, rpt);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntrospectRptParams {\n");
    
    sb.append("    oxdId: ").append(toIndentedString(oxdId)).append("\n");
    sb.append("    rpt: ").append(toIndentedString(rpt)).append("\n");
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

