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
 * UmaRsCheckAccessResponseData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T15:27:32.160Z")
public class UmaRsCheckAccessResponseData {
  @SerializedName("access")
  private String access = null;

  public UmaRsCheckAccessResponseData access(String access) {
    this.access = access;
    return this;
  }

   /**
   * Possible values are granted, denied
   * @return access
  **/
  @ApiModelProperty(example = "granted", required = true, value = "Possible values are granted, denied")
  public String getAccess() {
    return access;
  }

  public void setAccess(String access) {
    this.access = access;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UmaRsCheckAccessResponseData umaRsCheckAccessResponseData = (UmaRsCheckAccessResponseData) o;
    return Objects.equals(this.access, umaRsCheckAccessResponseData.access);
  }

  @Override
  public int hashCode() {
    return Objects.hash(access);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UmaRsCheckAccessResponseData {\n");
    
    sb.append("    access: ").append(toIndentedString(access)).append("\n");
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

