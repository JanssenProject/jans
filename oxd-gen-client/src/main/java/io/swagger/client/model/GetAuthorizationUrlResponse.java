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

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * GetAuthorizationUrlResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-03-15T09:55:53.588Z")
public class GetAuthorizationUrlResponse {
  @SerializedName("authorization_url")
  private String authorizationUrl = null;

  public GetAuthorizationUrlResponse authorizationUrl(String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
    return this;
  }

   /**
   * Get authorizationUrl
   * @return authorizationUrl
  **/
  @ApiModelProperty(example = "https://<op-hostname>/oxauth/restv1/authorize?response_type=code&client_id=@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!8A36.24E1.97DE.F4EF&redirect_uri=https://192.168.200.95/&scope=openid+profile+email+uma_protection+uma_authorization&state=473ot4nuqb4ubeokc139raur13&nonce=lbrdgorr974q66q6q9g454iccm", required = true, value = "")
  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  public void setAuthorizationUrl(String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetAuthorizationUrlResponse getAuthorizationUrlResponse = (GetAuthorizationUrlResponse) o;
    return Objects.equals(this.authorizationUrl, getAuthorizationUrlResponse.authorizationUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationUrl);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetAuthorizationUrlResponse {\n");
    
    sb.append("    authorizationUrl: ").append(toIndentedString(authorizationUrl)).append("\n");
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

