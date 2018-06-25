package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.GetAccessTokenByRefreshTokenResponseData;
import java.io.IOException;

/**
 * GetAccessTokenByRefreshTokenResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class GetAccessTokenByRefreshTokenResponse {
  @SerializedName("status")
  private String status = null;

  @SerializedName("data")
  private GetAccessTokenByRefreshTokenResponseData data = null;

  public GetAccessTokenByRefreshTokenResponse status(String status) {
    this.status = status;
    return this;
  }

   /**
   * Get status
   * @return status
  **/
  @ApiModelProperty(example = "ok", required = true, value = "")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public GetAccessTokenByRefreshTokenResponse data(GetAccessTokenByRefreshTokenResponseData data) {
    this.data = data;
    return this;
  }

   /**
   * Get data
   * @return data
  **/
  @ApiModelProperty(required = true, value = "")
  public GetAccessTokenByRefreshTokenResponseData getData() {
    return data;
  }

  public void setData(GetAccessTokenByRefreshTokenResponseData data) {
    this.data = data;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetAccessTokenByRefreshTokenResponse getAccessTokenByRefreshTokenResponse = (GetAccessTokenByRefreshTokenResponse) o;
    return Objects.equals(this.status, getAccessTokenByRefreshTokenResponse.status) &&
        Objects.equals(this.data, getAccessTokenByRefreshTokenResponse.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, data);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetAccessTokenByRefreshTokenResponse {\n");
    
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
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

