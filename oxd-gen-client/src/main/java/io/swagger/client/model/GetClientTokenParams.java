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
 * GetClientTokenParams
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class GetClientTokenParams {
  @SerializedName("op_host")
  private String opHost = null;

  @SerializedName("op_discovery_path")
  private String opDiscoveryPath = null;

  @SerializedName("scope")
  private List<String> scope = null;

  @SerializedName("client_id")
  private String clientId = null;

  @SerializedName("client_secret")
  private String clientSecret = null;

  @SerializedName("authentication_method")
  private String authenticationMethod = null;

  @SerializedName("algorithm")
  private String algorithm = null;

  @SerializedName("key_id")
  private String keyId = null;

  public GetClientTokenParams opHost(String opHost) {
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

  public GetClientTokenParams opDiscoveryPath(String opDiscoveryPath) {
    this.opDiscoveryPath = opDiscoveryPath;
    return this;
  }

   /**
   * Get opDiscoveryPath
   * @return opDiscoveryPath
  **/
  @ApiModelProperty(value = "")
  public String getOpDiscoveryPath() {
    return opDiscoveryPath;
  }

  public void setOpDiscoveryPath(String opDiscoveryPath) {
    this.opDiscoveryPath = opDiscoveryPath;
  }

  public GetClientTokenParams scope(List<String> scope) {
    this.scope = scope;
    return this;
  }

  public GetClientTokenParams addScopeItem(String scopeItem) {
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

  public GetClientTokenParams clientId(String clientId) {
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

  public GetClientTokenParams clientSecret(String clientSecret) {
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

  public GetClientTokenParams authenticationMethod(String authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
    return this;
  }

   /**
   * if value is missed then basic authentication is used. Otherwise it&#39;s possible to set &#x60;private_key_jwt&#x60; value for Private Key authentication.
   * @return authenticationMethod
  **/
  @ApiModelProperty(value = "if value is missed then basic authentication is used. Otherwise it's possible to set `private_key_jwt` value for Private Key authentication.")
  public String getAuthenticationMethod() {
    return authenticationMethod;
  }

  public void setAuthenticationMethod(String authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
  }

  public GetClientTokenParams algorithm(String algorithm) {
    this.algorithm = algorithm;
    return this;
  }

   /**
   * optional but is required if authentication_method&#x3D;private_key_jwt. Valid values are none, HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512
   * @return algorithm
  **/
  @ApiModelProperty(value = "optional but is required if authentication_method=private_key_jwt. Valid values are none, HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512")
  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public GetClientTokenParams keyId(String keyId) {
    this.keyId = keyId;
    return this;
  }

   /**
   * optional but is required if authentication_method&#x3D;private_key_jwt. It has to be valid key id from key store.
   * @return keyId
  **/
  @ApiModelProperty(value = "optional but is required if authentication_method=private_key_jwt. It has to be valid key id from key store.")
  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetClientTokenParams getClientTokenParams = (GetClientTokenParams) o;
    return Objects.equals(this.opHost, getClientTokenParams.opHost) &&
        Objects.equals(this.opDiscoveryPath, getClientTokenParams.opDiscoveryPath) &&
        Objects.equals(this.scope, getClientTokenParams.scope) &&
        Objects.equals(this.clientId, getClientTokenParams.clientId) &&
        Objects.equals(this.clientSecret, getClientTokenParams.clientSecret) &&
        Objects.equals(this.authenticationMethod, getClientTokenParams.authenticationMethod) &&
        Objects.equals(this.algorithm, getClientTokenParams.algorithm) &&
        Objects.equals(this.keyId, getClientTokenParams.keyId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(opHost, opDiscoveryPath, scope, clientId, clientSecret, authenticationMethod, algorithm, keyId);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetClientTokenParams {\n");
    
    sb.append("    opHost: ").append(toIndentedString(opHost)).append("\n");
    sb.append("    opDiscoveryPath: ").append(toIndentedString(opDiscoveryPath)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    clientSecret: ").append(toIndentedString(clientSecret)).append("\n");
    sb.append("    authenticationMethod: ").append(toIndentedString(authenticationMethod)).append("\n");
    sb.append("    algorithm: ").append(toIndentedString(algorithm)).append("\n");
    sb.append("    keyId: ").append(toIndentedString(keyId)).append("\n");
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

