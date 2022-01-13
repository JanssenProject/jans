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
 * RemoveSiteResponse
 */


public class RemoveSiteResponse {
  @SerializedName("rp_id")
  private String rpId = null;

  public RemoveSiteResponse rpId(String rpId) {
    this.rpId = rpId;
    return this;
  }

   /**
   * Get rpId
   * @return rpId
  **/
  @Schema(required = true, description = "")
  public String getRpId() {
    return rpId;
  }

  public void setRpId(String rpId) {
    this.rpId = rpId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RemoveSiteResponse removeSiteResponse = (RemoveSiteResponse) o;
    return Objects.equals(this.rpId, removeSiteResponse.rpId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rpId);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RemoveSiteResponse {\n");
    
    sb.append("    rpId: ").append(toIndentedString(rpId)).append("\n");
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
