/*
 * oxd-server
 * oxd-server
 *
 * OpenAPI spec version: 4.0.0
 * Contact: yuriyz@gluu.org
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


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
 * GetLogoutUriResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-10-31T01:45:57.530Z")
public class GetLogoutUriResponse {
  @SerializedName("uri")
  private String uri = null;

  public GetLogoutUriResponse uri(String uri) {
    this.uri = uri;
    return this;
  }

   /**
   * Get uri
   * @return uri
  **/
  @ApiModelProperty(example = "https://<op-hostname>/oxauth/seam/resource/restv1/oxauth/end_session?id_token_hint=eyJraWQiOiI1YmM2ZGM3MS0xYjA1LTQ5YzMtYWU3MC0zYTg4Y2ZiMjQwN2QiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2NlLWRldi5nbHV1Lm9yZyIsImF1ZCI6IkAhNUE1OC5BRTBELkQzODMuMUU0NiEwMDAxIUUzOEIuN0RCRSEwMDA4IUE3MTkuOTU4QS41QjdGLkVBQkMiLCJleHAiOjE0OTAwMTk5MjEsImlhdCI6MTQ5MDAxNjMyMSwibm9uY2UiOiJkNGdsbmtndHAxYWlqZ3JnY3V2cGp1N2k3cCIsImF1dGhfdGltZSI6MTQ5MDAxNjI3MiwiYXRfaGFzaCI6Im1Xa2NXQzZ6NC1qN0ZNX0ctX0tYMWciLCJveFZhbGlkYXRpb25VUkkiOiJodHRwczovL2NlLWRldi5nbHV1Lm9yZy9veGF1dGgvb3BpZnJhbWUiLCJveE9wZW5JRENvbm5lY3RWZXJzaW9uIjoib3BlbmlkY29ubmVjdC0xLjAiLCJzdWIiOiJONHRLRncyLVpDWTVWN0FhQmdpMnNHRWdDR0t0Tlg2LS01M2FQbmZFYk5zIn0.PvCdzPnMwqPNUw1bzd8tvzpJqYu-P2iCTnELr85ZaJTG8_Fdj3EruLgUBa-emeum3j29cFgdjFPx6WplfCV1GnehOieXjDiAAE85fy-stxXwII3xrva5ZjG0FnTYnJLoRmy0BWMjFC2IdCoISJI9imcfvmQmlvNmU0EjLS02cJf3JAaqEaM-FJWdQv8end9-Sq2bcp6ME3voRjV30ps_7jcDdlM_hW3M_e3RdrXYCDifbl_1jaNip5tb6_bLpgTADDoLT3fTvACRN057e2GCkSYdxvVhIjfDsjnOhk5n3TDcWedriu99H8-sNXyI_aBr3HAXd37CsgmdfIJcgUNJJw", required = true, value = "")
  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetLogoutUriResponse getLogoutUriResponse = (GetLogoutUriResponse) o;
    return Objects.equals(this.uri, getLogoutUriResponse.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetLogoutUriResponse {\n");
    
    sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
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

