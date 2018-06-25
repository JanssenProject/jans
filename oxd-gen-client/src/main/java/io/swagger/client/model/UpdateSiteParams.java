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
 * UpdateSiteParams
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T15:27:32.160Z")
public class UpdateSiteParams {
  @SerializedName("oxd_id")
  private String oxdId = null;

  @SerializedName("authorization_redirect_uri")
  private String authorizationRedirectUri = null;

  @SerializedName("post_logout_redirect_uri")
  private String postLogoutRedirectUri = null;

  @SerializedName("response_types")
  private List<String> responseTypes = null;

  @SerializedName("grant_types")
  private List<String> grantTypes = null;

  @SerializedName("scope")
  private List<String> scope = null;

  @SerializedName("acr_values")
  private List<String> acrValues = null;

  @SerializedName("client_jwks_uri")
  private String clientJwksUri = null;

  @SerializedName("client_token_endpoint_auth_method")
  private String clientTokenEndpointAuthMethod = null;

  @SerializedName("client_request_uris")
  private List<String> clientRequestUris = null;

  @SerializedName("client_sector_identifier_uri")
  private List<String> clientSectorIdentifierUri = null;

  @SerializedName("client_secret_expires_at")
  private Integer clientSecretExpiresAt = null;

  @SerializedName("contacts")
  private List<String> contacts = null;

  @SerializedName("ui_locales")
  private List<String> uiLocales = null;

  @SerializedName("claims_locales")
  private List<String> claimsLocales = null;

  public UpdateSiteParams oxdId(String oxdId) {
    this.oxdId = oxdId;
    return this;
  }

   /**
   * Get oxdId
   * @return oxdId
  **/
  @ApiModelProperty(example = "6F9619FF-8B86-D011-B42D-00CF4FC964FF", required = true, value = "")
  public String getOxdId() {
    return oxdId;
  }

  public void setOxdId(String oxdId) {
    this.oxdId = oxdId;
  }

  public UpdateSiteParams authorizationRedirectUri(String authorizationRedirectUri) {
    this.authorizationRedirectUri = authorizationRedirectUri;
    return this;
  }

   /**
   * Get authorizationRedirectUri
   * @return authorizationRedirectUri
  **/
  @ApiModelProperty(example = "https://client.example.org/cb", value = "")
  public String getAuthorizationRedirectUri() {
    return authorizationRedirectUri;
  }

  public void setAuthorizationRedirectUri(String authorizationRedirectUri) {
    this.authorizationRedirectUri = authorizationRedirectUri;
  }

  public UpdateSiteParams postLogoutRedirectUri(String postLogoutRedirectUri) {
    this.postLogoutRedirectUri = postLogoutRedirectUri;
    return this;
  }

   /**
   * Get postLogoutRedirectUri
   * @return postLogoutRedirectUri
  **/
  @ApiModelProperty(example = "https://client.example.org/cb", value = "")
  public String getPostLogoutRedirectUri() {
    return postLogoutRedirectUri;
  }

  public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
    this.postLogoutRedirectUri = postLogoutRedirectUri;
  }

  public UpdateSiteParams responseTypes(List<String> responseTypes) {
    this.responseTypes = responseTypes;
    return this;
  }

  public UpdateSiteParams addResponseTypesItem(String responseTypesItem) {
    if (this.responseTypes == null) {
      this.responseTypes = new ArrayList<String>();
    }
    this.responseTypes.add(responseTypesItem);
    return this;
  }

   /**
   * Get responseTypes
   * @return responseTypes
  **/
  @ApiModelProperty(example = "[\"code\"]", value = "")
  public List<String> getResponseTypes() {
    return responseTypes;
  }

  public void setResponseTypes(List<String> responseTypes) {
    this.responseTypes = responseTypes;
  }

  public UpdateSiteParams grantTypes(List<String> grantTypes) {
    this.grantTypes = grantTypes;
    return this;
  }

  public UpdateSiteParams addGrantTypesItem(String grantTypesItem) {
    if (this.grantTypes == null) {
      this.grantTypes = new ArrayList<String>();
    }
    this.grantTypes.add(grantTypesItem);
    return this;
  }

   /**
   * Get grantTypes
   * @return grantTypes
  **/
  @ApiModelProperty(example = "[\"authorization_code\",\"client_credentials\"]", value = "")
  public List<String> getGrantTypes() {
    return grantTypes;
  }

  public void setGrantTypes(List<String> grantTypes) {
    this.grantTypes = grantTypes;
  }

  public UpdateSiteParams scope(List<String> scope) {
    this.scope = scope;
    return this;
  }

  public UpdateSiteParams addScopeItem(String scopeItem) {
    if (this.scope == null) {
      this.scope = new ArrayList<String>();
    }
    this.scope.add(scopeItem);
    return this;
  }

   /**
   * Get scope
   * @return scope
  **/
  @ApiModelProperty(example = "[\"openid\"]", value = "")
  public List<String> getScope() {
    return scope;
  }

  public void setScope(List<String> scope) {
    this.scope = scope;
  }

  public UpdateSiteParams acrValues(List<String> acrValues) {
    this.acrValues = acrValues;
    return this;
  }

  public UpdateSiteParams addAcrValuesItem(String acrValuesItem) {
    if (this.acrValues == null) {
      this.acrValues = new ArrayList<String>();
    }
    this.acrValues.add(acrValuesItem);
    return this;
  }

   /**
   * Get acrValues
   * @return acrValues
  **/
  @ApiModelProperty(example = "[\"basic\"]", value = "")
  public List<String> getAcrValues() {
    return acrValues;
  }

  public void setAcrValues(List<String> acrValues) {
    this.acrValues = acrValues;
  }

  public UpdateSiteParams clientJwksUri(String clientJwksUri) {
    this.clientJwksUri = clientJwksUri;
    return this;
  }

   /**
   * Get clientJwksUri
   * @return clientJwksUri
  **/
  @ApiModelProperty(value = "")
  public String getClientJwksUri() {
    return clientJwksUri;
  }

  public void setClientJwksUri(String clientJwksUri) {
    this.clientJwksUri = clientJwksUri;
  }

  public UpdateSiteParams clientTokenEndpointAuthMethod(String clientTokenEndpointAuthMethod) {
    this.clientTokenEndpointAuthMethod = clientTokenEndpointAuthMethod;
    return this;
  }

   /**
   * Get clientTokenEndpointAuthMethod
   * @return clientTokenEndpointAuthMethod
  **/
  @ApiModelProperty(value = "")
  public String getClientTokenEndpointAuthMethod() {
    return clientTokenEndpointAuthMethod;
  }

  public void setClientTokenEndpointAuthMethod(String clientTokenEndpointAuthMethod) {
    this.clientTokenEndpointAuthMethod = clientTokenEndpointAuthMethod;
  }

  public UpdateSiteParams clientRequestUris(List<String> clientRequestUris) {
    this.clientRequestUris = clientRequestUris;
    return this;
  }

  public UpdateSiteParams addClientRequestUrisItem(String clientRequestUrisItem) {
    if (this.clientRequestUris == null) {
      this.clientRequestUris = new ArrayList<String>();
    }
    this.clientRequestUris.add(clientRequestUrisItem);
    return this;
  }

   /**
   * Get clientRequestUris
   * @return clientRequestUris
  **/
  @ApiModelProperty(value = "")
  public List<String> getClientRequestUris() {
    return clientRequestUris;
  }

  public void setClientRequestUris(List<String> clientRequestUris) {
    this.clientRequestUris = clientRequestUris;
  }

  public UpdateSiteParams clientSectorIdentifierUri(List<String> clientSectorIdentifierUri) {
    this.clientSectorIdentifierUri = clientSectorIdentifierUri;
    return this;
  }

  public UpdateSiteParams addClientSectorIdentifierUriItem(String clientSectorIdentifierUriItem) {
    if (this.clientSectorIdentifierUri == null) {
      this.clientSectorIdentifierUri = new ArrayList<String>();
    }
    this.clientSectorIdentifierUri.add(clientSectorIdentifierUriItem);
    return this;
  }

   /**
   * Get clientSectorIdentifierUri
   * @return clientSectorIdentifierUri
  **/
  @ApiModelProperty(value = "")
  public List<String> getClientSectorIdentifierUri() {
    return clientSectorIdentifierUri;
  }

  public void setClientSectorIdentifierUri(List<String> clientSectorIdentifierUri) {
    this.clientSectorIdentifierUri = clientSectorIdentifierUri;
  }

  public UpdateSiteParams clientSecretExpiresAt(Integer clientSecretExpiresAt) {
    this.clientSecretExpiresAt = clientSecretExpiresAt;
    return this;
  }

   /**
   * the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this Date object
   * @return clientSecretExpiresAt
  **/
  @ApiModelProperty(example = "1335205592410", value = "the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this Date object")
  public Integer getClientSecretExpiresAt() {
    return clientSecretExpiresAt;
  }

  public void setClientSecretExpiresAt(Integer clientSecretExpiresAt) {
    this.clientSecretExpiresAt = clientSecretExpiresAt;
  }

  public UpdateSiteParams contacts(List<String> contacts) {
    this.contacts = contacts;
    return this;
  }

  public UpdateSiteParams addContactsItem(String contactsItem) {
    if (this.contacts == null) {
      this.contacts = new ArrayList<String>();
    }
    this.contacts.add(contactsItem);
    return this;
  }

   /**
   * Get contacts
   * @return contacts
  **/
  @ApiModelProperty(example = "[\"foo_bar@spam.org\"]", value = "")
  public List<String> getContacts() {
    return contacts;
  }

  public void setContacts(List<String> contacts) {
    this.contacts = contacts;
  }

  public UpdateSiteParams uiLocales(List<String> uiLocales) {
    this.uiLocales = uiLocales;
    return this;
  }

  public UpdateSiteParams addUiLocalesItem(String uiLocalesItem) {
    if (this.uiLocales == null) {
      this.uiLocales = new ArrayList<String>();
    }
    this.uiLocales.add(uiLocalesItem);
    return this;
  }

   /**
   * Get uiLocales
   * @return uiLocales
  **/
  @ApiModelProperty(value = "")
  public List<String> getUiLocales() {
    return uiLocales;
  }

  public void setUiLocales(List<String> uiLocales) {
    this.uiLocales = uiLocales;
  }

  public UpdateSiteParams claimsLocales(List<String> claimsLocales) {
    this.claimsLocales = claimsLocales;
    return this;
  }

  public UpdateSiteParams addClaimsLocalesItem(String claimsLocalesItem) {
    if (this.claimsLocales == null) {
      this.claimsLocales = new ArrayList<String>();
    }
    this.claimsLocales.add(claimsLocalesItem);
    return this;
  }

   /**
   * Get claimsLocales
   * @return claimsLocales
  **/
  @ApiModelProperty(value = "")
  public List<String> getClaimsLocales() {
    return claimsLocales;
  }

  public void setClaimsLocales(List<String> claimsLocales) {
    this.claimsLocales = claimsLocales;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateSiteParams updateSiteParams = (UpdateSiteParams) o;
    return Objects.equals(this.oxdId, updateSiteParams.oxdId) &&
        Objects.equals(this.authorizationRedirectUri, updateSiteParams.authorizationRedirectUri) &&
        Objects.equals(this.postLogoutRedirectUri, updateSiteParams.postLogoutRedirectUri) &&
        Objects.equals(this.responseTypes, updateSiteParams.responseTypes) &&
        Objects.equals(this.grantTypes, updateSiteParams.grantTypes) &&
        Objects.equals(this.scope, updateSiteParams.scope) &&
        Objects.equals(this.acrValues, updateSiteParams.acrValues) &&
        Objects.equals(this.clientJwksUri, updateSiteParams.clientJwksUri) &&
        Objects.equals(this.clientTokenEndpointAuthMethod, updateSiteParams.clientTokenEndpointAuthMethod) &&
        Objects.equals(this.clientRequestUris, updateSiteParams.clientRequestUris) &&
        Objects.equals(this.clientSectorIdentifierUri, updateSiteParams.clientSectorIdentifierUri) &&
        Objects.equals(this.clientSecretExpiresAt, updateSiteParams.clientSecretExpiresAt) &&
        Objects.equals(this.contacts, updateSiteParams.contacts) &&
        Objects.equals(this.uiLocales, updateSiteParams.uiLocales) &&
        Objects.equals(this.claimsLocales, updateSiteParams.claimsLocales);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oxdId, authorizationRedirectUri, postLogoutRedirectUri, responseTypes, grantTypes, scope, acrValues, clientJwksUri, clientTokenEndpointAuthMethod, clientRequestUris, clientSectorIdentifierUri, clientSecretExpiresAt, contacts, uiLocales, claimsLocales);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateSiteParams {\n");
    
    sb.append("    oxdId: ").append(toIndentedString(oxdId)).append("\n");
    sb.append("    authorizationRedirectUri: ").append(toIndentedString(authorizationRedirectUri)).append("\n");
    sb.append("    postLogoutRedirectUri: ").append(toIndentedString(postLogoutRedirectUri)).append("\n");
    sb.append("    responseTypes: ").append(toIndentedString(responseTypes)).append("\n");
    sb.append("    grantTypes: ").append(toIndentedString(grantTypes)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    acrValues: ").append(toIndentedString(acrValues)).append("\n");
    sb.append("    clientJwksUri: ").append(toIndentedString(clientJwksUri)).append("\n");
    sb.append("    clientTokenEndpointAuthMethod: ").append(toIndentedString(clientTokenEndpointAuthMethod)).append("\n");
    sb.append("    clientRequestUris: ").append(toIndentedString(clientRequestUris)).append("\n");
    sb.append("    clientSectorIdentifierUri: ").append(toIndentedString(clientSectorIdentifierUri)).append("\n");
    sb.append("    clientSecretExpiresAt: ").append(toIndentedString(clientSecretExpiresAt)).append("\n");
    sb.append("    contacts: ").append(toIndentedString(contacts)).append("\n");
    sb.append("    uiLocales: ").append(toIndentedString(uiLocales)).append("\n");
    sb.append("    claimsLocales: ").append(toIndentedString(claimsLocales)).append("\n");
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

