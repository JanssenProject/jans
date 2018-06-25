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
 * IntrospectAccessTokenReponseData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T15:27:32.160Z")
public class IntrospectAccessTokenReponseData {
  @SerializedName("active")
  private Boolean active = null;

  @SerializedName("client_id")
  private String clientId = null;

  @SerializedName("username")
  private String username = null;

  @SerializedName("scopes")
  private List<String> scopes = new ArrayList<String>();

  @SerializedName("token_type")
  private String tokenType = null;

  @SerializedName("sub")
  private String sub = null;

  @SerializedName("aud")
  private String aud = null;

  @SerializedName("iss")
  private String iss = null;

  @SerializedName("exp")
  private Integer exp = null;

  @SerializedName("iat")
  private Integer iat = null;

  @SerializedName("acr_values")
  private List<String> acrValues = new ArrayList<String>();

  @SerializedName("extension_field")
  private String extensionField = null;

  public IntrospectAccessTokenReponseData active(Boolean active) {
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

  public IntrospectAccessTokenReponseData clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

   /**
   * Get clientId
   * @return clientId
  **/
  @ApiModelProperty(example = "@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387", required = true, value = "")
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public IntrospectAccessTokenReponseData username(String username) {
    this.username = username;
    return this;
  }

   /**
   * Get username
   * @return username
  **/
  @ApiModelProperty(example = "John Black", required = true, value = "")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public IntrospectAccessTokenReponseData scopes(List<String> scopes) {
    this.scopes = scopes;
    return this;
  }

  public IntrospectAccessTokenReponseData addScopesItem(String scopesItem) {
    this.scopes.add(scopesItem);
    return this;
  }

   /**
   * Get scopes
   * @return scopes
  **/
  @ApiModelProperty(required = true, value = "")
  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }

  public IntrospectAccessTokenReponseData tokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

   /**
   * Get tokenType
   * @return tokenType
  **/
  @ApiModelProperty(example = "bearer", required = true, value = "")
  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public IntrospectAccessTokenReponseData sub(String sub) {
    this.sub = sub;
    return this;
  }

   /**
   * Get sub
   * @return sub
  **/
  @ApiModelProperty(example = "jblack", required = true, value = "")
  public String getSub() {
    return sub;
  }

  public void setSub(String sub) {
    this.sub = sub;
  }

  public IntrospectAccessTokenReponseData aud(String aud) {
    this.aud = aud;
    return this;
  }

   /**
   * Get aud
   * @return aud
  **/
  @ApiModelProperty(example = "l238j323ds-23ij4", required = true, value = "")
  public String getAud() {
    return aud;
  }

  public void setAud(String aud) {
    this.aud = aud;
  }

  public IntrospectAccessTokenReponseData iss(String iss) {
    this.iss = iss;
    return this;
  }

   /**
   * Get iss
   * @return iss
  **/
  @ApiModelProperty(example = "https://as.gluu.org/", required = true, value = "")
  public String getIss() {
    return iss;
  }

  public void setIss(String iss) {
    this.iss = iss;
  }

  public IntrospectAccessTokenReponseData exp(Integer exp) {
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

  public IntrospectAccessTokenReponseData iat(Integer iat) {
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

  public IntrospectAccessTokenReponseData acrValues(List<String> acrValues) {
    this.acrValues = acrValues;
    return this;
  }

  public IntrospectAccessTokenReponseData addAcrValuesItem(String acrValuesItem) {
    this.acrValues.add(acrValuesItem);
    return this;
  }

   /**
   * Get acrValues
   * @return acrValues
  **/
  @ApiModelProperty(example = "[\"basic\"]", required = true, value = "")
  public List<String> getAcrValues() {
    return acrValues;
  }

  public void setAcrValues(List<String> acrValues) {
    this.acrValues = acrValues;
  }

  public IntrospectAccessTokenReponseData extensionField(String extensionField) {
    this.extensionField = extensionField;
    return this;
  }

   /**
   * Get extensionField
   * @return extensionField
  **/
  @ApiModelProperty(example = "twenty-seven", required = true, value = "")
  public String getExtensionField() {
    return extensionField;
  }

  public void setExtensionField(String extensionField) {
    this.extensionField = extensionField;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntrospectAccessTokenReponseData introspectAccessTokenReponseData = (IntrospectAccessTokenReponseData) o;
    return Objects.equals(this.active, introspectAccessTokenReponseData.active) &&
        Objects.equals(this.clientId, introspectAccessTokenReponseData.clientId) &&
        Objects.equals(this.username, introspectAccessTokenReponseData.username) &&
        Objects.equals(this.scopes, introspectAccessTokenReponseData.scopes) &&
        Objects.equals(this.tokenType, introspectAccessTokenReponseData.tokenType) &&
        Objects.equals(this.sub, introspectAccessTokenReponseData.sub) &&
        Objects.equals(this.aud, introspectAccessTokenReponseData.aud) &&
        Objects.equals(this.iss, introspectAccessTokenReponseData.iss) &&
        Objects.equals(this.exp, introspectAccessTokenReponseData.exp) &&
        Objects.equals(this.iat, introspectAccessTokenReponseData.iat) &&
        Objects.equals(this.acrValues, introspectAccessTokenReponseData.acrValues) &&
        Objects.equals(this.extensionField, introspectAccessTokenReponseData.extensionField);
  }

  @Override
  public int hashCode() {
    return Objects.hash(active, clientId, username, scopes, tokenType, sub, aud, iss, exp, iat, acrValues, extensionField);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntrospectAccessTokenReponseData {\n");
    
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
    sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
    sb.append("    sub: ").append(toIndentedString(sub)).append("\n");
    sb.append("    aud: ").append(toIndentedString(aud)).append("\n");
    sb.append("    iss: ").append(toIndentedString(iss)).append("\n");
    sb.append("    exp: ").append(toIndentedString(exp)).append("\n");
    sb.append("    iat: ").append(toIndentedString(iat)).append("\n");
    sb.append("    acrValues: ").append(toIndentedString(acrValues)).append("\n");
    sb.append("    extensionField: ").append(toIndentedString(extensionField)).append("\n");
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

