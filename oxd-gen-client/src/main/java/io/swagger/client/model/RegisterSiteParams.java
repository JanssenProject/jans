package io.swagger.client.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * RegisterSiteParams
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2018-07-25T18:54:39.321+05:30")
public class RegisterSiteParams   {
  @SerializedName("authorization_redirect_uri")
  private String authorizationRedirectUri = null;

  @SerializedName("op_host")
  private String opHost = null;

  @SerializedName("post_logout_redirect_uri")
  private String postLogoutRedirectUri = null;

  @SerializedName("application_type")
  private String applicationType = null;

  @SerializedName("response_types")
  private List<String> responseTypes = new ArrayList<String>();

  @SerializedName("grant_types")
  private List<String> grantTypes = new ArrayList<String>();

  @SerializedName("scope")
  private List<String> scope = new ArrayList<String>();

  @SerializedName("acr_values")
  private List<String> acrValues = new ArrayList<String>();

  @SerializedName("client_name")
  private String clientName = null;

  @SerializedName("client_jwks_uri")
  private String clientJwksUri = null;

  @SerializedName("client_token_endpoint_auth_method")
  private String clientTokenEndpointAuthMethod = null;

  @SerializedName("client_request_uris")
  private List<String> clientRequestUris = new ArrayList<String>();

  @SerializedName("client_frontchannel_logout_uris")
  private List<String> clientFrontchannelLogoutUris = new ArrayList<String>();

  @SerializedName("clientSector_identifier_uri")
  private String clientSectorIdentifierUri = new String();

  @SerializedName("contacts")
  private List<String> contacts = new ArrayList<String>();

  @SerializedName("redirect_uris")
  private List<String> redirectUris = new ArrayList<String>();

  @SerializedName("ui_locales")
  private List<String> uiLocales = new ArrayList<String>();

  @SerializedName("claims_locales")
  private List<String> claimsLocales = new ArrayList<String>();

  @SerializedName("claims_redirect_uri")
  private List<String> claimsRedirectUri = new ArrayList<String>();

  @SerializedName("client_id")
  private String clientId = null;

  @SerializedName("client_secret")
  private String clientSecret = null;

  @SerializedName("trusted_client")
  private Boolean trustedClient = null;

  public RegisterSiteParams authorizationRedirectUri(String authorizationRedirectUri) {
    this.authorizationRedirectUri = authorizationRedirectUri;
    return this;
  }

  /**
   * Get authorizationRedirectUri
   * @return authorizationRedirectUri
   **/
  @ApiModelProperty(example = "https://client.example.org/cb", required = true, value = "")
  public String getAuthorizationRedirectUri() {
    return authorizationRedirectUri;
  }

  public void setAuthorizationRedirectUri(String authorizationRedirectUri) {
    this.authorizationRedirectUri = authorizationRedirectUri;
  }

  public RegisterSiteParams opHost(String opHost) {
    this.opHost = opHost;
    return this;
  }

  /**
   * If missing, must be present in defaults
   * @return opHost
   **/
  @ApiModelProperty(example = "https://&lt;ophostname&gt;", value = "If missing, must be present in defaults")
  public String getOpHost() {
    return opHost;
  }

  public void setOpHost(String opHost) {
    this.opHost = opHost;
  }

  public RegisterSiteParams postLogoutRedirectUri(String postLogoutRedirectUri) {
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

  public RegisterSiteParams applicationType(String applicationType) {
    this.applicationType = applicationType;
    return this;
  }

  /**
   * Get applicationType
   * @return applicationType
   **/
  @ApiModelProperty(example = "web", value = "")
  public String getApplicationType() {
    return applicationType;
  }

  public void setApplicationType(String applicationType) {
    this.applicationType = applicationType;
  }

  public RegisterSiteParams responseTypes(List<String> responseTypes) {
    this.responseTypes = responseTypes;
    return this;
  }

  public RegisterSiteParams addResponseTypesItem(String responseTypesItem) {
    this.responseTypes.add(responseTypesItem);
    return this;
  }

  /**
   * Get responseTypes
   * @return responseTypes
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getResponseTypes() {
    return responseTypes;
  }

  public void setResponseTypes(List<String> responseTypes) {
    this.responseTypes = responseTypes;
  }

  public RegisterSiteParams grantTypes(List<String> grantTypes) {
    this.grantTypes = grantTypes;
    return this;
  }

  public RegisterSiteParams addGrantTypesItem(String grantTypesItem) {
    this.grantTypes.add(grantTypesItem);
    return this;
  }

  /**
   * Get grantTypes
   * @return grantTypes
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getGrantTypes() {
    return grantTypes;
  }

  public void setGrantTypes(List<String> grantTypes) {
    this.grantTypes = grantTypes;
  }

  public RegisterSiteParams scope(List<String> scope) {
    this.scope = scope;
    return this;
  }

  public RegisterSiteParams addScopeItem(String scopeItem) {
    this.scope.add(scopeItem);
    return this;
  }

  /**
   * Get scope
   * @return scope
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getScope() {
    return scope;
  }

  public void setScope(List<String> scope) {
    this.scope = scope;
  }

  public RegisterSiteParams acrValues(List<String> acrValues) {
    this.acrValues = acrValues;
    return this;
  }

  public RegisterSiteParams addAcrValuesItem(String acrValuesItem) {
    this.acrValues.add(acrValuesItem);
    return this;
  }

  /**
   * Get acrValues
   * @return acrValues
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getAcrValues() {
    return acrValues;
  }

  public void setAcrValues(List<String> acrValues) {
    this.acrValues = acrValues;
  }

  public RegisterSiteParams clientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

  /**
   * oxd will generate its own non-human readable name by defaultif client_name is not specified
   * @return clientName
   **/
  @ApiModelProperty(example = "null", value = "oxd will generate its own non-human readable name by defaultif client_name is not specified")
  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public RegisterSiteParams clientJwksUri(String clientJwksUri) {
    this.clientJwksUri = clientJwksUri;
    return this;
  }

  /**
   * Get clientJwksUri
   * @return clientJwksUri
   **/
  @ApiModelProperty(example = "null", value = "")
  public String getClientJwksUri() {
    return clientJwksUri;
  }

  public void setClientJwksUri(String clientJwksUri) {
    this.clientJwksUri = clientJwksUri;
  }

  public RegisterSiteParams clientTokenEndpointAuthMethod(String clientTokenEndpointAuthMethod) {
    this.clientTokenEndpointAuthMethod = clientTokenEndpointAuthMethod;
    return this;
  }

  /**
   * Get clientTokenEndpointAuthMethod
   * @return clientTokenEndpointAuthMethod
   **/
  @ApiModelProperty(example = "null", value = "")
  public String getClientTokenEndpointAuthMethod() {
    return clientTokenEndpointAuthMethod;
  }

  public void setClientTokenEndpointAuthMethod(String clientTokenEndpointAuthMethod) {
    this.clientTokenEndpointAuthMethod = clientTokenEndpointAuthMethod;
  }

  public RegisterSiteParams clientRequestUris(List<String> clientRequestUris) {
    this.clientRequestUris = clientRequestUris;
    return this;
  }

  public RegisterSiteParams addClientRequestUrisItem(String clientRequestUrisItem) {
    this.clientRequestUris.add(clientRequestUrisItem);
    return this;
  }

  /**
   * Get clientRequestUris
   * @return clientRequestUris
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getClientRequestUris() {
    return clientRequestUris;
  }

  public void setClientRequestUris(List<String> clientRequestUris) {
    this.clientRequestUris = clientRequestUris;
  }

  public RegisterSiteParams clientFrontchannelLogoutUris(List<String> clientFrontchannelLogoutUris) {
    this.clientFrontchannelLogoutUris = clientFrontchannelLogoutUris;
    return this;
  }

  public RegisterSiteParams addClientFrontchannelLogoutUrisItem(String clientFrontchannelLogoutUrisItem) {
    this.clientFrontchannelLogoutUris.add(clientFrontchannelLogoutUrisItem);
    return this;
  }

  /**
   * Get clientFrontchannelLogoutUris
   * @return clientFrontchannelLogoutUris
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getClientFrontchannelLogoutUris() {
    return clientFrontchannelLogoutUris;
  }

  public void setClientFrontchannelLogoutUris(List<String> clientFrontchannelLogoutUris) {
    this.clientFrontchannelLogoutUris = clientFrontchannelLogoutUris;
  }

  public RegisterSiteParams clientSectorIdentifierUri(String clientSectorIdentifierUri) {
    this.clientSectorIdentifierUri = clientSectorIdentifierUri;
    return this;
  }


  /**
   * Get clientSectorIdentifierUri
   * @return clientSectorIdentifierUri
   **/
  @ApiModelProperty(example = "null", value = "")
  public String getClientSectorIdentifierUri() {
    return clientSectorIdentifierUri;
  }

  public void setClientSectorIdentifierUri(String clientSectorIdentifierUri) {
    this.clientSectorIdentifierUri = clientSectorIdentifierUri;
  }

  public RegisterSiteParams contacts(List<String> contacts) {
    this.contacts = contacts;
    return this;
  }

  public RegisterSiteParams addContactsItem(String contactsItem) {
    this.contacts.add(contactsItem);
    return this;
  }

  /**
   * Get contacts
   * @return contacts
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getContacts() {
    return contacts;
  }

  public void setContacts(List<String> contacts) {
    this.contacts = contacts;
  }

  public RegisterSiteParams redirectUris(List<String> redirectUris) {
    this.redirectUris = redirectUris;
    return this;
  }

  public RegisterSiteParams addRedirectUrisItem(String redirectUrisItem) {
    this.redirectUris.add(redirectUrisItem);
    return this;
  }

  /**
   * Get redirectUris
   * @return redirectUris
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getRedirectUris() {
    return redirectUris;
  }

  public void setRedirectUris(List<String> redirectUris) {
    this.redirectUris = redirectUris;
  }

  public RegisterSiteParams uiLocales(List<String> uiLocales) {
    this.uiLocales = uiLocales;
    return this;
  }

  public RegisterSiteParams addUiLocalesItem(String uiLocalesItem) {
    this.uiLocales.add(uiLocalesItem);
    return this;
  }

  /**
   * Get uiLocales
   * @return uiLocales
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getUiLocales() {
    return uiLocales;
  }

  public void setUiLocales(List<String> uiLocales) {
    this.uiLocales = uiLocales;
  }

  public RegisterSiteParams claimsLocales(List<String> claimsLocales) {
    this.claimsLocales = claimsLocales;
    return this;
  }

  public RegisterSiteParams addClaimsLocalesItem(String claimsLocalesItem) {
    this.claimsLocales.add(claimsLocalesItem);
    return this;
  }

  /**
   * Get claimsLocales
   * @return claimsLocales
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getClaimsLocales() {
    return claimsLocales;
  }

  public void setClaimsLocales(List<String> claimsLocales) {
    this.claimsLocales = claimsLocales;
  }

  public RegisterSiteParams claimsRedirectUri(List<String> claimsRedirectUri) {
    this.claimsRedirectUri = claimsRedirectUri;
    return this;
  }

  public RegisterSiteParams addClaimsRedirectUriItem(String claimsRedirectUriItem) {
    this.claimsRedirectUri.add(claimsRedirectUriItem);
    return this;
  }

  /**
   * Get claimsRedirectUri
   * @return claimsRedirectUri
   **/
  @ApiModelProperty(example = "null", value = "")
  public List<String> getClaimsRedirectUri() {
    return claimsRedirectUri;
  }

  public void setClaimsRedirectUri(List<String> claimsRedirectUri) {
    this.claimsRedirectUri = claimsRedirectUri;
  }

  public RegisterSiteParams clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * client id of existing client, ignores all other parameters and skips new client registration forcing to use existing client (client_secret is required if this parameter is set)
   * @return clientId
   **/
  @ApiModelProperty(example = "null", value = "client id of existing client, ignores all other parameters and skips new client registration forcing to use existing client (client_secret is required if this parameter is set)")
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public RegisterSiteParams clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  /**
   * client secret of existing client, must be used together with client_id
   * @return clientSecret
   **/
  @ApiModelProperty(example = "null", value = "client secret of existing client, must be used together with client_id")
  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public RegisterSiteParams trustedClient(Boolean trustedClient) {
    this.trustedClient = trustedClient;
    return this;
  }

  /**
   * specified whether client is trusted. Default value is false.
   * @return trustedClient
   **/
  @ApiModelProperty(example = "null", value = "specified whether client is trusted. Default value is false.")
  public Boolean getTrustedClient() {
    return trustedClient;
  }

  public void setTrustedClient(Boolean trustedClient) {
    this.trustedClient = trustedClient;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegisterSiteParams registerSiteParams = (RegisterSiteParams) o;
    return Objects.equals(this.authorizationRedirectUri, registerSiteParams.authorizationRedirectUri) &&
            Objects.equals(this.opHost, registerSiteParams.opHost) &&
            Objects.equals(this.postLogoutRedirectUri, registerSiteParams.postLogoutRedirectUri) &&
            Objects.equals(this.applicationType, registerSiteParams.applicationType) &&
            Objects.equals(this.responseTypes, registerSiteParams.responseTypes) &&
            Objects.equals(this.grantTypes, registerSiteParams.grantTypes) &&
            Objects.equals(this.scope, registerSiteParams.scope) &&
            Objects.equals(this.acrValues, registerSiteParams.acrValues) &&
            Objects.equals(this.clientName, registerSiteParams.clientName) &&
            Objects.equals(this.clientJwksUri, registerSiteParams.clientJwksUri) &&
            Objects.equals(this.clientTokenEndpointAuthMethod, registerSiteParams.clientTokenEndpointAuthMethod) &&
            Objects.equals(this.clientRequestUris, registerSiteParams.clientRequestUris) &&
            Objects.equals(this.clientFrontchannelLogoutUris, registerSiteParams.clientFrontchannelLogoutUris) &&
            Objects.equals(this.clientSectorIdentifierUri, registerSiteParams.clientSectorIdentifierUri) &&
            Objects.equals(this.contacts, registerSiteParams.contacts) &&
            Objects.equals(this.redirectUris, registerSiteParams.redirectUris) &&
            Objects.equals(this.uiLocales, registerSiteParams.uiLocales) &&
            Objects.equals(this.claimsLocales, registerSiteParams.claimsLocales) &&
            Objects.equals(this.claimsRedirectUri, registerSiteParams.claimsRedirectUri) &&
            Objects.equals(this.clientId, registerSiteParams.clientId) &&
            Objects.equals(this.clientSecret, registerSiteParams.clientSecret) &&
            Objects.equals(this.trustedClient, registerSiteParams.trustedClient);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationRedirectUri, opHost, postLogoutRedirectUri, applicationType, responseTypes, grantTypes, scope, acrValues, clientName, clientJwksUri, clientTokenEndpointAuthMethod, clientRequestUris, clientFrontchannelLogoutUris, clientSectorIdentifierUri, contacts, redirectUris, uiLocales, claimsLocales, claimsRedirectUri, clientId, clientSecret, trustedClient);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegisterSiteParams {\n");

    sb.append("    authorizationRedirectUri: ").append(toIndentedString(authorizationRedirectUri)).append("\n");
    sb.append("    opHost: ").append(toIndentedString(opHost)).append("\n");
    sb.append("    postLogoutRedirectUri: ").append(toIndentedString(postLogoutRedirectUri)).append("\n");
    sb.append("    applicationType: ").append(toIndentedString(applicationType)).append("\n");
    sb.append("    responseTypes: ").append(toIndentedString(responseTypes)).append("\n");
    sb.append("    grantTypes: ").append(toIndentedString(grantTypes)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    acrValues: ").append(toIndentedString(acrValues)).append("\n");
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    clientJwksUri: ").append(toIndentedString(clientJwksUri)).append("\n");
    sb.append("    clientTokenEndpointAuthMethod: ").append(toIndentedString(clientTokenEndpointAuthMethod)).append("\n");
    sb.append("    clientRequestUris: ").append(toIndentedString(clientRequestUris)).append("\n");
    sb.append("    clientFrontchannelLogoutUris: ").append(toIndentedString(clientFrontchannelLogoutUris)).append("\n");
    sb.append("    clientSectorIdentifierUri: ").append(toIndentedString(clientSectorIdentifierUri)).append("\n");
    sb.append("    contacts: ").append(toIndentedString(contacts)).append("\n");
    sb.append("    redirectUris: ").append(toIndentedString(redirectUris)).append("\n");
    sb.append("    uiLocales: ").append(toIndentedString(uiLocales)).append("\n");
    sb.append("    claimsLocales: ").append(toIndentedString(claimsLocales)).append("\n");
    sb.append("    claimsRedirectUri: ").append(toIndentedString(claimsRedirectUri)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    clientSecret: ").append(toIndentedString(clientSecret)).append("\n");
    sb.append("    trustedClient: ").append(toIndentedString(trustedClient)).append("\n");
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

