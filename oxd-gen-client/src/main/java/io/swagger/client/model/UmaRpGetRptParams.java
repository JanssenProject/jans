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
import java.util.ArrayList;
import java.util.List;

/**
 * UmaRpGetRptParams
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class UmaRpGetRptParams {
  @SerializedName("oxd_id")
  private String oxdId = null;

  @SerializedName("ticket")
  private String ticket = null;

  @SerializedName("claim_token")
  private String claimToken = null;

  @SerializedName("claim_token_format")
  private String claimTokenFormat = null;

  @SerializedName("pct")
  private String pct = null;

  @SerializedName("rpt")
  private String rpt = null;

  @SerializedName("scope")
  private List<String> scope = new ArrayList<String>();

  @SerializedName("state")
  private String state = null;

  public UmaRpGetRptParams oxdId(String oxdId) {
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

  public UmaRpGetRptParams ticket(String ticket) {
    this.ticket = ticket;
    return this;
  }

   /**
   * Get ticket
   * @return ticket
  **/
  @ApiModelProperty(required = true, value = "")
  public String getTicket() {
    return ticket;
  }

  public void setTicket(String ticket) {
    this.ticket = ticket;
  }

  public UmaRpGetRptParams claimToken(String claimToken) {
    this.claimToken = claimToken;
    return this;
  }

   /**
   * Get claimToken
   * @return claimToken
  **/
  @ApiModelProperty(required = true, value = "")
  public String getClaimToken() {
    return claimToken;
  }

  public void setClaimToken(String claimToken) {
    this.claimToken = claimToken;
  }

  public UmaRpGetRptParams claimTokenFormat(String claimTokenFormat) {
    this.claimTokenFormat = claimTokenFormat;
    return this;
  }

   /**
   * Get claimTokenFormat
   * @return claimTokenFormat
  **/
  @ApiModelProperty(required = true, value = "")
  public String getClaimTokenFormat() {
    return claimTokenFormat;
  }

  public void setClaimTokenFormat(String claimTokenFormat) {
    this.claimTokenFormat = claimTokenFormat;
  }

  public UmaRpGetRptParams pct(String pct) {
    this.pct = pct;
    return this;
  }

   /**
   * Get pct
   * @return pct
  **/
  @ApiModelProperty(required = true, value = "")
  public String getPct() {
    return pct;
  }

  public void setPct(String pct) {
    this.pct = pct;
  }

  public UmaRpGetRptParams rpt(String rpt) {
    this.rpt = rpt;
    return this;
  }

   /**
   * Get rpt
   * @return rpt
  **/
  @ApiModelProperty(required = true, value = "")
  public String getRpt() {
    return rpt;
  }

  public void setRpt(String rpt) {
    this.rpt = rpt;
  }

  public UmaRpGetRptParams scope(List<String> scope) {
    this.scope = scope;
    return this;
  }

  public UmaRpGetRptParams addScopeItem(String scopeItem) {
    this.scope.add(scopeItem);
    return this;
  }

   /**
   * Get scope
   * @return scope
  **/
  @ApiModelProperty(example = "[\"openid\"]", required = true, value = "")
  public List<String> getScope() {
    return scope;
  }

  public void setScope(List<String> scope) {
    this.scope = scope;
  }

  public UmaRpGetRptParams state(String state) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UmaRpGetRptParams umaRpGetRptParams = (UmaRpGetRptParams) o;
    return Objects.equals(this.oxdId, umaRpGetRptParams.oxdId) &&
        Objects.equals(this.ticket, umaRpGetRptParams.ticket) &&
        Objects.equals(this.claimToken, umaRpGetRptParams.claimToken) &&
        Objects.equals(this.claimTokenFormat, umaRpGetRptParams.claimTokenFormat) &&
        Objects.equals(this.pct, umaRpGetRptParams.pct) &&
        Objects.equals(this.rpt, umaRpGetRptParams.rpt) &&
        Objects.equals(this.scope, umaRpGetRptParams.scope) &&
        Objects.equals(this.state, umaRpGetRptParams.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oxdId, ticket, claimToken, claimTokenFormat, pct, rpt, scope, state);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UmaRpGetRptParams {\n");
    
    sb.append("    oxdId: ").append(toIndentedString(oxdId)).append("\n");
    sb.append("    ticket: ").append(toIndentedString(ticket)).append("\n");
    sb.append("    claimToken: ").append(toIndentedString(claimToken)).append("\n");
    sb.append("    claimTokenFormat: ").append(toIndentedString(claimTokenFormat)).append("\n");
    sb.append("    pct: ").append(toIndentedString(pct)).append("\n");
    sb.append("    rpt: ").append(toIndentedString(rpt)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
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

