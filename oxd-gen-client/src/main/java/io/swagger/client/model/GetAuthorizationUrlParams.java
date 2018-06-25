package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.GetauthorizationurlCustomParameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GetAuthorizationUrlParams
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class GetAuthorizationUrlParams {
  @SerializedName("oxd_id")
  private String oxdId = null;

  @SerializedName("scope")
  private List<String> scope = null;

  @SerializedName("acr_values")
  private List<String> acrValues = null;

  @SerializedName("prompt")
  private String prompt = null;

  @SerializedName("custom_parameters")
  private GetauthorizationurlCustomParameters customParameters = null;

  public GetAuthorizationUrlParams oxdId(String oxdId) {
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

  public GetAuthorizationUrlParams scope(List<String> scope) {
    this.scope = scope;
    return this;
  }

  public GetAuthorizationUrlParams addScopeItem(String scopeItem) {
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

  public GetAuthorizationUrlParams acrValues(List<String> acrValues) {
    this.acrValues = acrValues;
    return this;
  }

  public GetAuthorizationUrlParams addAcrValuesItem(String acrValuesItem) {
    if (this.acrValues == null) {
      this.acrValues = new ArrayList<String>();
    }
    this.acrValues.add(acrValuesItem);
    return this;
  }

   /**
   * Get acrValues
   * @return acrValues
  **/
  @ApiModelProperty(example = "[\"basic\"]", value = "")
  public List<String> getAcrValues() {
    return acrValues;
  }

  public void setAcrValues(List<String> acrValues) {
    this.acrValues = acrValues;
  }

  public GetAuthorizationUrlParams prompt(String prompt) {
    this.prompt = prompt;
    return this;
  }

   /**
   * Get prompt
   * @return prompt
  **/
  @ApiModelProperty(value = "")
  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public GetAuthorizationUrlParams customParameters(GetauthorizationurlCustomParameters customParameters) {
    this.customParameters = customParameters;
    return this;
  }

   /**
   * Get customParameters
   * @return customParameters
  **/
  @ApiModelProperty(value = "")
  public GetauthorizationurlCustomParameters getCustomParameters() {
    return customParameters;
  }

  public void setCustomParameters(GetauthorizationurlCustomParameters customParameters) {
    this.customParameters = customParameters;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetAuthorizationUrlParams getAuthorizationUrlParams = (GetAuthorizationUrlParams) o;
    return Objects.equals(this.oxdId, getAuthorizationUrlParams.oxdId) &&
        Objects.equals(this.scope, getAuthorizationUrlParams.scope) &&
        Objects.equals(this.acrValues, getAuthorizationUrlParams.acrValues) &&
        Objects.equals(this.prompt, getAuthorizationUrlParams.prompt) &&
        Objects.equals(this.customParameters, getAuthorizationUrlParams.customParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oxdId, scope, acrValues, prompt, customParameters);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetAuthorizationUrlParams {\n");
    
    sb.append("    oxdId: ").append(toIndentedString(oxdId)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    acrValues: ").append(toIndentedString(acrValues)).append("\n");
    sb.append("    prompt: ").append(toIndentedString(prompt)).append("\n");
    sb.append("    customParameters: ").append(toIndentedString(customParameters)).append("\n");
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

