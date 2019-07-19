/*
 * oxd-server
 * oxd-server
 *
 * OpenAPI spec version: 4.0
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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * RegisterSiteResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-03-15T09:55:53.588Z")
public class RegisterSiteResponse {
  @SerializedName("oxd_id")
  private String oxdId = null;

  @SerializedName("op_host")
  private String opHost = null;

  @SerializedName("client_id")
  private String clientId = null;

  @SerializedName("client_name")
  private String clientName = null;

  @SerializedName("client_secret")
  private String clientSecret = null;

  @SerializedName("client_registration_access_token")
  private String clientRegistrationAccessToken = null;

  @SerializedName("client_registration_client_uri")
  private String clientRegistrationClientUri = null;

  @SerializedName("client_id_issued_at")
  private Integer clientIdIssuedAt = null;

  @SerializedName("client_secret_expires_at")
  private Integer clientSecretExpiresAt = null;

  public RegisterSiteResponse oxdId(String oxdId) {
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

  public RegisterSiteResponse opHost(String opHost) {
    this.opHost = opHost;
    return this;
  }

   /**
   * Get opHost
   * @return opHost
  **/
  @ApiModelProperty(example = "https://<op-hostname>", required = true, value = "")
  public String getOpHost() {
    return opHost;
  }

  public void setOpHost(String opHost) {
    this.opHost = opHost;
  }

  public RegisterSiteResponse clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

   /**
   * Get clientId
   * @return clientId
  **/
  @ApiModelProperty(example = "@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387", value = "")
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public RegisterSiteResponse clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  /**
   * Get clientName
   * @return clientName
   **/
  @ApiModelProperty(example = "TestClientName", value = "")
  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName= clientName;
  }

  public RegisterSiteResponse clientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

   /**
   * Get clientSecret
   * @return clientSecret
  **/
  @ApiModelProperty(example = "f436b936-03fc-433f-9772-53c2bc9e1c74", value = "")
  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public RegisterSiteResponse clientRegistrationAccessToken(String clientRegistrationAccessToken) {
    this.clientRegistrationAccessToken = clientRegistrationAccessToken;
    return this;
  }

   /**
   * Get clientRegistrationAccessToken
   * @return clientRegistrationAccessToken
  **/
  @ApiModelProperty(example = "d836df94-44b0-445a-848a-d43189839b17", value = "")
  public String getClientRegistrationAccessToken() {
    return clientRegistrationAccessToken;
  }

  public void setClientRegistrationAccessToken(String clientRegistrationAccessToken) {
    this.clientRegistrationAccessToken = clientRegistrationAccessToken;
  }

  public RegisterSiteResponse clientRegistrationClientUri(String clientRegistrationClientUri) {
    this.clientRegistrationClientUri = clientRegistrationClientUri;
    return this;
  }

   /**
   * Get clientRegistrationClientUri
   * @return clientRegistrationClientUri
  **/
  @ApiModelProperty(example = "https://<op-hostname>/oxauth/restv1/register?client_id=@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387", value = "")
  public String getClientRegistrationClientUri() {
    return clientRegistrationClientUri;
  }

  public void setClientRegistrationClientUri(String clientRegistrationClientUri) {
    this.clientRegistrationClientUri = clientRegistrationClientUri;
  }

  public RegisterSiteResponse clientIdIssuedAt(Integer clientIdIssuedAt) {
    this.clientIdIssuedAt = clientIdIssuedAt;
    return this;
  }

   /**
   * Get clientIdIssuedAt
   * @return clientIdIssuedAt
  **/
  @ApiModelProperty(example = "1501854943", value = "")
  public Integer getClientIdIssuedAt() {
    return clientIdIssuedAt;
  }

  public void setClientIdIssuedAt(Integer clientIdIssuedAt) {
    this.clientIdIssuedAt = clientIdIssuedAt;
  }

  public RegisterSiteResponse clientSecretExpiresAt(Integer clientSecretExpiresAt) {
    this.clientSecretExpiresAt = clientSecretExpiresAt;
    return this;
  }

   /**
   * Get clientSecretExpiresAt
   * @return clientSecretExpiresAt
  **/
  @ApiModelProperty(example = "1501941343", value = "")
  public Integer getClientSecretExpiresAt() {
    return clientSecretExpiresAt;
  }

  public void setClientSecretExpiresAt(Integer clientSecretExpiresAt) {
    this.clientSecretExpiresAt = clientSecretExpiresAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RegisterSiteResponse that = (RegisterSiteResponse) o;
    return Objects.equals(oxdId, that.oxdId) &&
            Objects.equals(opHost, that.opHost) &&
            Objects.equals(clientId, that.clientId) &&
            Objects.equals(clientName, that.clientName) &&
            Objects.equals(clientSecret, that.clientSecret) &&
            Objects.equals(clientRegistrationAccessToken, that.clientRegistrationAccessToken) &&
            Objects.equals(clientRegistrationClientUri, that.clientRegistrationClientUri) &&
            Objects.equals(clientIdIssuedAt, that.clientIdIssuedAt) &&
            Objects.equals(clientSecretExpiresAt, that.clientSecretExpiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oxdId, opHost, clientId, clientName, clientSecret, clientRegistrationAccessToken, clientRegistrationClientUri, clientIdIssuedAt, clientSecretExpiresAt);
  }

  @Override
  public String toString() {
    return "RegisterSiteResponse{" +
            "oxdId='" + oxdId + '\'' +
            ", opHost='" + opHost + '\'' +
            ", clientId='" + clientId + '\'' +
            ", clientName='" + clientName + '\'' +
            ", clientSecret='" + clientSecret + '\'' +
            ", clientRegistrationAccessToken='" + clientRegistrationAccessToken + '\'' +
            ", clientRegistrationClientUri='" + clientRegistrationClientUri + '\'' +
            ", clientIdIssuedAt=" + clientIdIssuedAt +
            ", clientSecretExpiresAt=" + clientSecretExpiresAt +
            '}';
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

