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
import io.swagger.client.model.JsonWebKey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * GetRpJwksResponse
 */


public class GetRpJwksResponse {
  @SerializedName("keys")
  private List<JsonWebKey> keys = new ArrayList<JsonWebKey>();

  public GetRpJwksResponse keys(List<JsonWebKey> keys) {
    this.keys = keys;
    return this;
  }

  public GetRpJwksResponse addKeysItem(JsonWebKey keysItem) {
    this.keys.add(keysItem);
    return this;
  }

   /**
   * Get keys
   * @return keys
  **/
  @Schema(required = true, description = "")
  public List<JsonWebKey> getKeys() {
    return keys;
  }

  public void setKeys(List<JsonWebKey> keys) {
    this.keys = keys;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetRpJwksResponse getRpJwksResponse = (GetRpJwksResponse) o;
    return Objects.equals(this.keys, getRpJwksResponse.keys);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keys);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetRpJwksResponse {\n");
    
    sb.append("    keys: ").append(toIndentedString(keys)).append("\n");
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
