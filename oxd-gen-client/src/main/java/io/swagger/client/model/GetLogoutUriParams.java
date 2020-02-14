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
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * GetLogoutUriParams
 */


public class GetLogoutUriParams {
  @SerializedName("oxd_id")
  private String oxdId = null;

  @SerializedName("id_token_hint")
  private String idTokenHint = null;

  @SerializedName("post_logout_redirect_uri")
  private String postLogoutRedirectUri = null;

  @SerializedName("state")
  private String state = null;

  @SerializedName("session_state")
  private String sessionState = null;

  public GetLogoutUriParams oxdId(String oxdId) {
    this.oxdId = oxdId;
    return this;
  }

   /**
   * Get oxdId
   * @return oxdId
  **/
  @Schema(example = "bcad760f-91ba-46e1-a020-05e4281d91b6", required = true, description = "")
  public String getOxdId() {
    return oxdId;
  }

  public void setOxdId(String oxdId) {
    this.oxdId = oxdId;
  }

  public GetLogoutUriParams idTokenHint(String idTokenHint) {
    this.idTokenHint = idTokenHint;
    return this;
  }

   /**
   * Get idTokenHint
   * @return idTokenHint
  **/
  @Schema(example = "eyJ0 ... NiJ9.eyJ1c ... I6IjIifX0.DeWt4Qu ... ZXso", required = true, description = "")
  public String getIdTokenHint() {
    return idTokenHint;
  }

  public void setIdTokenHint(String idTokenHint) {
    this.idTokenHint = idTokenHint;
  }

  public GetLogoutUriParams postLogoutRedirectUri(String postLogoutRedirectUri) {
    this.postLogoutRedirectUri = postLogoutRedirectUri;
    return this;
  }

   /**
   * Get postLogoutRedirectUri
   * @return postLogoutRedirectUri
  **/
  @Schema(example = "https://client.example.org/cb", required = true, description = "")
  public String getPostLogoutRedirectUri() {
    return postLogoutRedirectUri;
  }

  public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
    this.postLogoutRedirectUri = postLogoutRedirectUri;
  }

  public GetLogoutUriParams state(String state) {
    this.state = state;
    return this;
  }

   /**
   * Get state
   * @return state
  **/
  @Schema(required = true, description = "")
  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public GetLogoutUriParams sessionState(String sessionState) {
    this.sessionState = sessionState;
    return this;
  }

   /**
   * Get sessionState
   * @return sessionState
  **/
  @Schema(required = true, description = "")
  public String getSessionState() {
    return sessionState;
  }

  public void setSessionState(String sessionState) {
    this.sessionState = sessionState;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetLogoutUriParams getLogoutUriParams = (GetLogoutUriParams) o;
    return Objects.equals(this.oxdId, getLogoutUriParams.oxdId) &&
        Objects.equals(this.idTokenHint, getLogoutUriParams.idTokenHint) &&
        Objects.equals(this.postLogoutRedirectUri, getLogoutUriParams.postLogoutRedirectUri) &&
        Objects.equals(this.state, getLogoutUriParams.state) &&
        Objects.equals(this.sessionState, getLogoutUriParams.sessionState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oxdId, idTokenHint, postLogoutRedirectUri, state, sessionState);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetLogoutUriParams {\n");
    
    sb.append("    oxdId: ").append(toIndentedString(oxdId)).append("\n");
    sb.append("    idTokenHint: ").append(toIndentedString(idTokenHint)).append("\n");
    sb.append("    postLogoutRedirectUri: ").append(toIndentedString(postLogoutRedirectUri)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    sessionState: ").append(toIndentedString(sessionState)).append("\n");
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
