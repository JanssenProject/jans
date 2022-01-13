/*
 * jans-api-server
 * jans-api-server
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
 * IntrospectRptParams
 */


public class IntrospectRptParams {
  @SerializedName("rp_id")
  private String rpId = null;

  @SerializedName("rpt")
  private String rpt = null;

  public IntrospectRptParams rpId(String rpId) {
    this.rpId = rpId;
    return this;
  }

   /**
   * Get rpId
   * @return rpId
  **/
  @Schema(example = "bcad760f-91ba-46e1-a020-05e4281d91b6", required = true, description = "")
  public String getRpId() {
    return rpId;
  }

  public void setRpId(String rpId) {
    this.rpId = rpId;
  }

  public IntrospectRptParams rpt(String rpt) {
    this.rpt = rpt;
    return this;
  }

   /**
   * Get rpt
   * @return rpt
  **/
  @Schema(required = true, description = "")
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
    return Objects.equals(this.rpId, introspectRptParams.rpId) &&
        Objects.equals(this.rpt, introspectRptParams.rpt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rpId, rpt);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntrospectRptParams {\n");
    
    sb.append("    rpId: ").append(toIndentedString(rpId)).append("\n");
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
