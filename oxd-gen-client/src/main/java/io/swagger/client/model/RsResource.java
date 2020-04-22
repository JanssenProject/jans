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
import io.swagger.client.model.Condition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * RsResource
 */


public class RsResource {
  @SerializedName("path")
  private String path = null;

  @SerializedName("conditions")
  private List<Condition> conditions = new ArrayList<Condition>();

  @SerializedName("exp")
  private Long exp = null;

  @SerializedName("iat")
  private Long iat = null;

  public RsResource path(String path) {
    this.path = path;
    return this;
  }

   /**
   * Get path
   * @return path
  **/
  @Schema(required = true, description = "")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public RsResource conditions(List<Condition> conditions) {
    this.conditions = conditions;
    return this;
  }

  public RsResource addConditionsItem(Condition conditionsItem) {
    this.conditions.add(conditionsItem);
    return this;
  }

   /**
   * Get conditions
   * @return conditions
  **/
  @Schema(required = true, description = "")
  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public RsResource exp(Long exp) {
    this.exp = exp;
    return this;
  }

   /**
   * Resource expiration date in terms of number of seconds since January 1 1970 UTC
   * @return exp
  **/
  @Schema(example = "1545709072", description = "Resource expiration date in terms of number of seconds since January 1 1970 UTC")
  public Long getExp() {
    return exp;
  }

  public void setExp(Long exp) {
    this.exp = exp;
  }

  public RsResource iat(Long iat) {
    this.iat = iat;
    return this;
  }

   /**
   * Resource creation date in terms of number of seconds since January 1 1970 UTC
   * @return iat
  **/
  @Schema(example = "1535709072", description = "Resource creation date in terms of number of seconds since January 1 1970 UTC")
  public Long getIat() {
    return iat;
  }

  public void setIat(Long iat) {
    this.iat = iat;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RsResource rsResource = (RsResource) o;
    return Objects.equals(this.path, rsResource.path) &&
        Objects.equals(this.conditions, rsResource.conditions) &&
        Objects.equals(this.exp, rsResource.exp) &&
        Objects.equals(this.iat, rsResource.iat);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, conditions, exp, iat);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RsResource {\n");
    
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    conditions: ").append(toIndentedString(conditions)).append("\n");
    sb.append("    exp: ").append(toIndentedString(exp)).append("\n");
    sb.append("    iat: ").append(toIndentedString(iat)).append("\n");
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
