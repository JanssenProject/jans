/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

/**
 * @author Puja Sharma
 *
 */
public class Backchannel implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Size(min=0)
	private String backchannelClientId;
	
	@Size(min=0)
	private String backchannelRedirectUri;
	
	@Size(min=0)
	private String backchannelAuthenticationEndpoint;
	
	@Size(min=0)
	private String backchannelDeviceRegistrationEndpoint;
	
	@Size(min=0)
	private List<String> backchannelTokenDeliveryModesSupported;
	
	@Size(min=0)
	private List<String> backchannelAuthenticationRequestSigningAlgValuesSupported;
	
	private Boolean backchannelUserCodeParameterSupported;
	
	@Size(min=0)
	private String backchannelBindingMessagePattern;
	
	@Min(value = 1, message = "Backchannel Authentication Response Expires In should not be less than 1")
	@Max(value = 2147483647, message = "Backchannel Authentication Response Expires In should not be greater than 2147483647")
	private int backchannelAuthenticationResponseExpiresIn;
	 
	@Min(value = 1, message = "Backchannel Authentication Response Interval In should not be less than 1")
	@Max(value = 2147483647, message = "Backchannel Authentication Interval Expires In should not be greater than 2147483647")
	private int backchannelAuthenticationResponseInterval;
	
	private List<String> backchannelLoginHintClaims;
	
	
	public String getBackchannelClientId() {
		return backchannelClientId;
	}
	
	public void setBackchannelClientId(String backchannelClientId) {
		this.backchannelClientId = backchannelClientId;
	}
	
	public String getBackchannelRedirectUri() {
		return backchannelRedirectUri;
	}
	
	public void setBackchannelRedirectUri(String backchannelRedirectUri) {
		this.backchannelRedirectUri = backchannelRedirectUri;
	}
	
	public String getBackchannelAuthenticationEndpoint() {
		return backchannelAuthenticationEndpoint;
	}
	
	public void setBackchannelAuthenticationEndpoint(String backchannelAuthenticationEndpoint) {
		this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
	}
	
	public String getBackchannelDeviceRegistrationEndpoint() {
		return backchannelDeviceRegistrationEndpoint;
	}
	
	public void setBackchannelDeviceRegistrationEndpoint(String backchannelDeviceRegistrationEndpoint) {
		this.backchannelDeviceRegistrationEndpoint = backchannelDeviceRegistrationEndpoint;
	}
	
	public List<String> getBackchannelTokenDeliveryModesSupported() {
		return backchannelTokenDeliveryModesSupported;
	}
	
	public void setBackchannelTokenDeliveryModesSupported(List<String> backchannelTokenDeliveryModesSupported) {
		this.backchannelTokenDeliveryModesSupported = backchannelTokenDeliveryModesSupported;
	}
	
	public List<String> getBackchannelAuthenticationRequestSigningAlgValuesSupported() {
		return backchannelAuthenticationRequestSigningAlgValuesSupported;
	}
	
	public void setBackchannelAuthenticationRequestSigningAlgValuesSupported(
			List<String> backchannelAuthenticationRequestSigningAlgValuesSupported) {
		this.backchannelAuthenticationRequestSigningAlgValuesSupported = backchannelAuthenticationRequestSigningAlgValuesSupported;
	}
	
	public Boolean getBackchannelUserCodeParameterSupported() {
		return backchannelUserCodeParameterSupported;
	}
	
	public void setBackchannelUserCodeParameterSupported(Boolean backchannelUserCodeParameterSupported) {
		this.backchannelUserCodeParameterSupported = backchannelUserCodeParameterSupported;
	}
	
	public String getBackchannelBindingMessagePattern() {
		return backchannelBindingMessagePattern;
	}
	
	public void setBackchannelBindingMessagePattern(String backchannelBindingMessagePattern) {
		this.backchannelBindingMessagePattern = backchannelBindingMessagePattern;
	}
	
	public int getBackchannelAuthenticationResponseExpiresIn() {
		return backchannelAuthenticationResponseExpiresIn;
	}
	
	public void setBackchannelAuthenticationResponseExpiresIn(int backchannelAuthenticationResponseExpiresIn) {
		this.backchannelAuthenticationResponseExpiresIn = backchannelAuthenticationResponseExpiresIn;
	}
	
	public int getBackchannelAuthenticationResponseInterval() {
		return backchannelAuthenticationResponseInterval;
	}
	
	public void setBackchannelAuthenticationResponseInterval(int backchannelAuthenticationResponseInterval) {
		this.backchannelAuthenticationResponseInterval = backchannelAuthenticationResponseInterval;
	}
	
	public List<String> getBackchannelLoginHintClaims() {
		return backchannelLoginHintClaims;
	}
	
	public void setBackchannelLoginHintClaims(List<String> backchannelLoginHintClaims) {
		this.backchannelLoginHintClaims = backchannelLoginHintClaims;
	}
		
}
