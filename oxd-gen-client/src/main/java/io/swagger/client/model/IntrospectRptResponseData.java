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
import java.util.ArrayList;
import java.util.List;

/**
 * IntrospectRptResponseData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T15:27:32.160Z")
public class IntrospectRptResponseData {
  @SerializedName("active")
  private Boolean active = null;

  @SerializedName("exp")
  private Integer exp = null;

  @SerializedName("iat")
  private Integer iat = null;

  @SerializedName("permissions")
  private List<Object> permissions = new ArrayList<Object>();

  public IntrospectRptResponseData active(Boolean active) {
    this.active = active;
    return this;
  }

   /**
   * Get active
   * @return active
  **/
  @ApiModelProperty(example = "true", required = true, value = "")
  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public IntrospectRptResponseData exp(Integer exp) {
    this.exp = exp;
    return this;
  }

   /**
   * Get exp
   * @return exp
  **/
  @ApiModelProperty(example = "299", required = true, value = "")
  public Integer getExp() {
    return exp;
  }

  public void setExp(Integer exp) {
    this.exp = exp;
  }

  public IntrospectRptResponseData iat(Integer iat) {
    this.iat = iat;
    return this;
  }

   /**
   * Get iat
   * @return iat
  **/
  @ApiModelProperty(example = "1419350238", required = true, value = "")
  public Integer getIat() {
    return iat;
  }

  public void setIat(Integer iat) {
    this.iat = iat;
  }

  public IntrospectRptResponseData permissions(List<Object> permissions) {
    this.permissions = permissions;
    return this;
  }

  public IntrospectRptResponseData addPermissionsItem(Object permissionsItem) {
    this.permissions.add(permissionsItem);
    return this;
  }

   /**
   * Get permissions
   * @return permissions
  **/
  @ApiModelProperty(required = true, value = "")
  public List<Object> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<Object> permissions) {
    this.permissions = permissions;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntrospectRptResponseData introspectRptResponseData = (IntrospectRptResponseData) o;
    return Objects.equals(this.active, introspectRptResponseData.active) &&
        Objects.equals(this.exp, introspectRptResponseData.exp) &&
        Objects.equals(this.iat, introspectRptResponseData.iat) &&
        Objects.equals(this.permissions, introspectRptResponseData.permissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(active, exp, iat, permissions);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntrospectRptResponseData {\n");
    
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    exp: ").append(toIndentedString(exp)).append("\n");
    sb.append("    iat: ").append(toIndentedString(iat)).append("\n");
    sb.append("    permissions: ").append(toIndentedString(permissions)).append("\n");
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

