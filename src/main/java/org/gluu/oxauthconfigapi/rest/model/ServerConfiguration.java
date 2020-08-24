package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.gluu.oxauth.model.error.ErrorHandlingMethod;

public class ServerConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@NotBlank
	@Size(min=1)
	private String oxId;
	private String cssLocation;
    private String jsLocation;
    private String imgLocation;	
	
	@Min(value=1)
	@Max(value=2147483647)
	private int configurationUpdateInterval;
	


	private List<String> personCustomObjectClassList;
    private Set<String> authorizationRequestCustomAllowedParameters;
	private ErrorHandlingMethod errorHandlingMethod = ErrorHandlingMethod.INTERNAL;
    private Boolean introspectionScriptBackwardCompatibility = false;
	private Boolean fapiCompatibility = false;
	private Boolean consentGatheringScriptBackwardCompatibility = false;
	private Boolean errorReasonEnabled  = false;
	private Boolean clientRegDefaultToCodeFlowWithRefresh;
	private Boolean logClientIdOnClientAuthentication;
	private Boolean logClientNameOnClientAuthentication;
	private Boolean disableU2fEndpoint = false;
	private Boolean useLocalCache = false;
	private Boolean cibaEnabled;
	
	public String getOxId() {
		return oxId;
	}
	
	public void setOxId(String oxId) {
		this.oxId = oxId;
	}
	
	public String getCssLocation() {
		return cssLocation;
	}
	
	public void setCssLocation(String cssLocation) {
		this.cssLocation = cssLocation;
	}
	public String getJsLocation() {
		return jsLocation;
	}
	
	public void setJsLocation(String jsLocation) {
		this.jsLocation = jsLocation;
	}
	public String getImgLocation() {
		return imgLocation;
	}
	
	public void setImgLocation(String imgLocation) {
		this.imgLocation = imgLocation;
	}
	
	public int getConfigurationUpdateInterval() {
		return configurationUpdateInterval;
	}
	
	public void setConfigurationUpdateInterval(int configurationUpdateInterval) {
		this.configurationUpdateInterval = configurationUpdateInterval;
	}
	
	public List<String> getPersonCustomObjectClassList() {
		return personCustomObjectClassList;
	}
	
	public void setPersonCustomObjectClassList(List<String> personCustomObjectClassList) {
		this.personCustomObjectClassList = personCustomObjectClassList;
	}
	
	public Set<String> getAuthorizationRequestCustomAllowedParameters() {
		return authorizationRequestCustomAllowedParameters;
	}
	
	public void setAuthorizationRequestCustomAllowedParameters(Set<String> authorizationRequestCustomAllowedParameters) {
		this.authorizationRequestCustomAllowedParameters = authorizationRequestCustomAllowedParameters;
	}
	
	public ErrorHandlingMethod getErrorHandlingMethod() {
		return errorHandlingMethod;
	}
	
	public void setErrorHandlingMethod(ErrorHandlingMethod errorHandlingMethod) {
		this.errorHandlingMethod = errorHandlingMethod;
	}
	
	public Boolean getIntrospectionScriptBackwardCompatibility() {
		return introspectionScriptBackwardCompatibility;
	}
	
	public void setIntrospectionScriptBackwardCompatibility(Boolean introspectionScriptBackwardCompatibility) {
		this.introspectionScriptBackwardCompatibility = introspectionScriptBackwardCompatibility;
	}
	
	public Boolean getFapiCompatibility() {
		return fapiCompatibility;
	}
	
	public void setFapiCompatibility(Boolean fapiCompatibility) {
		this.fapiCompatibility = fapiCompatibility;
	}
	
	public Boolean getConsentGatheringScriptBackwardCompatibility() {
		return consentGatheringScriptBackwardCompatibility;
	}
	
	public void setConsentGatheringScriptBackwardCompatibility(Boolean consentGatheringScriptBackwardCompatibility) {
		this.consentGatheringScriptBackwardCompatibility = consentGatheringScriptBackwardCompatibility;
	}
	
	public Boolean getErrorReasonEnabled() {
		return errorReasonEnabled;
	}
	
	public void setErrorReasonEnabled(Boolean errorReasonEnabled) {
		this.errorReasonEnabled = errorReasonEnabled;
	}
	
	public Boolean getClientRegDefaultToCodeFlowWithRefresh() {
		return clientRegDefaultToCodeFlowWithRefresh;
	}
	
	public void setClientRegDefaultToCodeFlowWithRefresh(Boolean clientRegDefaultToCodeFlowWithRefresh) {
		this.clientRegDefaultToCodeFlowWithRefresh = clientRegDefaultToCodeFlowWithRefresh;
	}
	
	public Boolean getLogClientIdOnClientAuthentication() {
		return logClientIdOnClientAuthentication;
	}
	
	public void setLogClientIdOnClientAuthentication(Boolean logClientIdOnClientAuthentication) {
		this.logClientIdOnClientAuthentication = logClientIdOnClientAuthentication;
	}
	
	public Boolean getLogClientNameOnClientAuthentication() {
		return logClientNameOnClientAuthentication;
	}
	
	public void setLogClientNameOnClientAuthentication(Boolean logClientNameOnClientAuthentication) {
		this.logClientNameOnClientAuthentication = logClientNameOnClientAuthentication;
	}
	
	public Boolean getDisableU2fEndpoint() {
		return disableU2fEndpoint;
	}
	
	public void setDisableU2fEndpoint(Boolean disableU2fEndpoint) {
		this.disableU2fEndpoint = disableU2fEndpoint;
	}
	
	public Boolean getUseLocalCache() {
		return useLocalCache;
	}
	
	public void setUseLocalCache(Boolean useLocalCache) {
		this.useLocalCache = useLocalCache;
	}
	
	public Boolean getCibaEnabled() {
		return cibaEnabled;
	}
	
	public void setCibaEnabled(Boolean cibaEnabled) {
		this.cibaEnabled = cibaEnabled;
	}
	
}
