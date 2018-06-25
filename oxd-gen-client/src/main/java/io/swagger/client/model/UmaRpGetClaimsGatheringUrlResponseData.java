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
 * UmaRpGetClaimsGatheringUrlResponseData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class UmaRpGetClaimsGatheringUrlResponseData {
  @SerializedName("url")
  private String url = null;

  @SerializedName("state")
  private String state = null;

  public UmaRpGetClaimsGatheringUrlResponseData url(String url) {
    this.url = url;
    return this;
  }

   /**
   * Get url
   * @return url
  **/
  @ApiModelProperty(example = "https://<op-hostname>/oxauth/restv1/uma/gather_claims?client_id@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!4508.BF20.9B81.E904&ticket=fba00191-59ab-4ed6-ac99-a786a88a9f40&claims_redirect_uri=https://client.example.com/cb&state=d871gpie16np0f5kfv936sc33k", required = true, value = "")
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public UmaRpGetClaimsGatheringUrlResponseData state(String state) {
    this.state = state;
    return this;
  }

   /**
   * Get state
   * @return state
  **/
  @ApiModelProperty(example = "d871gpie16np0f5kfv936sc33k", required = true, value = "")
  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UmaRpGetClaimsGatheringUrlResponseData umaRpGetClaimsGatheringUrlResponseData = (UmaRpGetClaimsGatheringUrlResponseData) o;
    return Objects.equals(this.url, umaRpGetClaimsGatheringUrlResponseData.url) &&
        Objects.equals(this.state, umaRpGetClaimsGatheringUrlResponseData.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, state);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UmaRpGetClaimsGatheringUrlResponseData {\n");
    
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
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

