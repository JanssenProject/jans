package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.GetUserInfoResponseClaims;
import java.io.IOException;

/**
 * GetUserInfoResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T15:27:32.160Z")
public class GetUserInfoResponse {
  @SerializedName("claims")
  private GetUserInfoResponseClaims claims = null;

  public GetUserInfoResponse claims(GetUserInfoResponseClaims claims) {
    this.claims = claims;
    return this;
  }

   /**
   * Get claims
   * @return claims
  **/
  @ApiModelProperty(required = true, value = "")
  public GetUserInfoResponseClaims getClaims() {
    return claims;
  }

  public void setClaims(GetUserInfoResponseClaims claims) {
    this.claims = claims;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetUserInfoResponse getUserInfoResponse = (GetUserInfoResponse) o;
    return Objects.equals(this.claims, getUserInfoResponse.claims);
  }

  @Override
  public int hashCode() {
    return Objects.hash(claims);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetUserInfoResponse {\n");
    
    sb.append("    claims: ").append(toIndentedString(claims)).append("\n");
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

