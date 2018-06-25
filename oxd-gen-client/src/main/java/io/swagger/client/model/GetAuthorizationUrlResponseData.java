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
 * GetAuthorizationUrlResponseData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class GetAuthorizationUrlResponseData {
  @SerializedName("authorization_url")
  private String authorizationUrl = null;

  public GetAuthorizationUrlResponseData authorizationUrl(String authorizationUrl) {
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
    GetAuthorizationUrlResponseData getAuthorizationUrlResponseData = (GetAuthorizationUrlResponseData) o;
    return Objects.equals(this.authorizationUrl, getAuthorizationUrlResponseData.authorizationUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationUrl);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetAuthorizationUrlResponseData {\n");
    
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

