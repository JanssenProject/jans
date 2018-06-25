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
 * SetupClientResponseData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class SetupClientResponseData {
  @SerializedName("oxd_id")
  private String oxdId = null;

  @SerializedName("client_id_of_oxd_id")
  private String clientIdOfOxdId = null;

  @SerializedName("op_host")
  private String opHost = null;

  @SerializedName("setup_client_oxd_id")
  private String setupClientOxdId = null;

  @SerializedName("client_id")
  private String clientId = null;

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

  public SetupClientResponseData oxdId(String oxdId) {
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

  public SetupClientResponseData clientIdOfOxdId(String clientIdOfOxdId) {
    this.clientIdOfOxdId = clientIdOfOxdId;
    return this;
  }

   /**
   * Get clientIdOfOxdId
   * @return clientIdOfOxdId
  **/
  @ApiModelProperty(example = "ccad760f-91ba-46e1-a020-05e4281d91b6", required = true, value = "")
  public String getClientIdOfOxdId() {
    return clientIdOfOxdId;
  }

  public void setClientIdOfOxdId(String clientIdOfOxdId) {
    this.clientIdOfOxdId = clientIdOfOxdId;
  }

  public SetupClientResponseData opHost(String opHost) {
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

  public SetupClientResponseData setupClientOxdId(String setupClientOxdId) {
    this.setupClientOxdId = setupClientOxdId;
    return this;
  }

   /**
   * Get setupClientOxdId
   * @return setupClientOxdId
  **/
  @ApiModelProperty(required = true, value = "")
  public String getSetupClientOxdId() {
    return setupClientOxdId;
  }

  public void setSetupClientOxdId(String setupClientOxdId) {
    this.setupClientOxdId = setupClientOxdId;
  }

  public SetupClientResponseData clientId(String clientId) {
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

  public SetupClientResponseData clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

   /**
   * Get clientSecret
   * @return clientSecret
  **/
  @ApiModelProperty(example = "f436b936-03fc-433f-9772-53c2bc9e1c74", required = true, value = "")
  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public SetupClientResponseData clientRegistrationAccessToken(String clientRegistrationAccessToken) {
    this.clientRegistrationAccessToken = clientRegistrationAccessToken;
    return this;
  }

   /**
   * Get clientRegistrationAccessToken
   * @return clientRegistrationAccessToken
  **/
  @ApiModelProperty(example = "d836df94-44b0-445a-848a-d43189839b17", required = true, value = "")
  public String getClientRegistrationAccessToken() {
    return clientRegistrationAccessToken;
  }

  public void setClientRegistrationAccessToken(String clientRegistrationAccessToken) {
    this.clientRegistrationAccessToken = clientRegistrationAccessToken;
  }

  public SetupClientResponseData clientRegistrationClientUri(String clientRegistrationClientUri) {
    this.clientRegistrationClientUri = clientRegistrationClientUri;
    return this;
  }

   /**
   * Get clientRegistrationClientUri
   * @return clientRegistrationClientUri
  **/
  @ApiModelProperty(example = "https://<op-hostname>/oxauth/restv1/register?client_id=@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387", required = true, value = "")
  public String getClientRegistrationClientUri() {
    return clientRegistrationClientUri;
  }

  public void setClientRegistrationClientUri(String clientRegistrationClientUri) {
    this.clientRegistrationClientUri = clientRegistrationClientUri;
  }

  public SetupClientResponseData clientIdIssuedAt(Integer clientIdIssuedAt) {
    this.clientIdIssuedAt = clientIdIssuedAt;
    return this;
  }

   /**
   * Get clientIdIssuedAt
   * @return clientIdIssuedAt
  **/
  @ApiModelProperty(example = "1501854943", required = true, value = "")
  public Integer getClientIdIssuedAt() {
    return clientIdIssuedAt;
  }

  public void setClientIdIssuedAt(Integer clientIdIssuedAt) {
    this.clientIdIssuedAt = clientIdIssuedAt;
  }

  public SetupClientResponseData clientSecretExpiresAt(Integer clientSecretExpiresAt) {
    this.clientSecretExpiresAt = clientSecretExpiresAt;
    return this;
  }

   /**
   * Get clientSecretExpiresAt
   * @return clientSecretExpiresAt
  **/
  @ApiModelProperty(example = "1501941343", required = true, value = "")
  public Integer getClientSecretExpiresAt() {
    return clientSecretExpiresAt;
  }

  public void setClientSecretExpiresAt(Integer clientSecretExpiresAt) {
    this.clientSecretExpiresAt = clientSecretExpiresAt;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetupClientResponseData setupClientResponseData = (SetupClientResponseData) o;
    return Objects.equals(this.oxdId, setupClientResponseData.oxdId) &&
        Objects.equals(this.clientIdOfOxdId, setupClientResponseData.clientIdOfOxdId) &&
        Objects.equals(this.opHost, setupClientResponseData.opHost) &&
        Objects.equals(this.setupClientOxdId, setupClientResponseData.setupClientOxdId) &&
        Objects.equals(this.clientId, setupClientResponseData.clientId) &&
        Objects.equals(this.clientSecret, setupClientResponseData.clientSecret) &&
        Objects.equals(this.clientRegistrationAccessToken, setupClientResponseData.clientRegistrationAccessToken) &&
        Objects.equals(this.clientRegistrationClientUri, setupClientResponseData.clientRegistrationClientUri) &&
        Objects.equals(this.clientIdIssuedAt, setupClientResponseData.clientIdIssuedAt) &&
        Objects.equals(this.clientSecretExpiresAt, setupClientResponseData.clientSecretExpiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oxdId, clientIdOfOxdId, opHost, setupClientOxdId, clientId, clientSecret, clientRegistrationAccessToken, clientRegistrationClientUri, clientIdIssuedAt, clientSecretExpiresAt);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetupClientResponseData {\n");
    
    sb.append("    oxdId: ").append(toIndentedString(oxdId)).append("\n");
    sb.append("    clientIdOfOxdId: ").append(toIndentedString(clientIdOfOxdId)).append("\n");
    sb.append("    opHost: ").append(toIndentedString(opHost)).append("\n");
    sb.append("    setupClientOxdId: ").append(toIndentedString(setupClientOxdId)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    clientSecret: ").append(toIndentedString(clientSecret)).append("\n");
    sb.append("    clientRegistrationAccessToken: ").append(toIndentedString(clientRegistrationAccessToken)).append("\n");
    sb.append("    clientRegistrationClientUri: ").append(toIndentedString(clientRegistrationClientUri)).append("\n");
    sb.append("    clientIdIssuedAt: ").append(toIndentedString(clientIdIssuedAt)).append("\n");
    sb.append("    clientSecretExpiresAt: ").append(toIndentedString(clientSecretExpiresAt)).append("\n");
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

