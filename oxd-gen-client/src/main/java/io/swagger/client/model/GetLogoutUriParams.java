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
 * GetLogoutUriParams
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
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
  @ApiModelProperty(example = "bcad760f-91ba-46e1-a020-05e4281d91b6", required = true, value = "")
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
  @ApiModelProperty(example = "eyJ0 ... NiJ9.eyJ1c ... I6IjIifX0.DeWt4Qu ... ZXso", required = true, value = "")
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
  @ApiModelProperty(example = "https://client.example.org/cb", required = true, value = "")
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
  @ApiModelProperty(required = true, value = "")
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
  @ApiModelProperty(required = true, value = "")
  public String getSessionState() {
    return sessionState;
  }

  public void setSessionState(String sessionState) {
    this.sessionState = sessionState;
  }


  @Override
  public boolean equals(java.lang.Object o) {
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
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

