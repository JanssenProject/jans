package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

public class UmaConfiguration implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	@NotBlank
	@Size(min=1)
	private String umaConfigurationEndpoint;
	
	@Positive
    @Min(value=1)
    @Max(value=2147483647)
	private int umaRptLifetime;
    
	@Positive
    @Min(value=1)
    @Max(value=2147483647)
    private int umaTicketLifetime;
    
    @Min(value=1)
    @Max(value=2147483647)
    private int umaPctLifetime;
    
    @Positive
    @Min(value=1)
    @Max(value=2147483647)
    private int umaResourceLifetime;
    
    private Boolean umaAddScopesAutomatically = false;
    private Boolean umaValidateClaimToken = false;
    private Boolean umaGrantAccessIfNoPolicies = false;
    private Boolean umaRestrictResourceToAssociatedClient = false;
    private Boolean umaRptAsJwt = false;
    
    public String getUmaConfigurationEndpoint() {
		return umaConfigurationEndpoint;
	}

	public void setUmaConfigurationEndpoint(String umaConfigurationEndpoint) {
		this.umaConfigurationEndpoint = umaConfigurationEndpoint;
	}

	public int getUmaRptLifetime() {
		return umaRptLifetime;
	}
	
	public void setUmaRptLifetime(int umaRptLifetime) {
		this.umaRptLifetime = umaRptLifetime;
	}
	
	public int getUmaTicketLifetime() {
		return umaTicketLifetime;
	}
	
	public void setUmaTicketLifetime(int umaTicketLifetime) {
		this.umaTicketLifetime = umaTicketLifetime;
	}
	
	public int getUmaPctLifetime() {
		return umaPctLifetime;
	}
	
	public void setUmaPctLifetime(int umaPctLifetime) {
		this.umaPctLifetime = umaPctLifetime;
	}
	
	public int getUmaResourceLifetime() {
		return umaResourceLifetime;
	}
	
	public void setUmaResourceLifetime(int umaResourceLifetime) {
		this.umaResourceLifetime = umaResourceLifetime;
	}
	
	public Boolean getUmaAddScopesAutomatically() {
		return umaAddScopesAutomatically;
	}
	
	public void setUmaAddScopesAutomatically(Boolean umaAddScopesAutomatically) {
		this.umaAddScopesAutomatically = umaAddScopesAutomatically;
	}
	
	public Boolean getUmaValidateClaimToken() {
		return umaValidateClaimToken;
	}
	
	public void setUmaValidateClaimToken(Boolean umaValidateClaimToken) {
		this.umaValidateClaimToken = umaValidateClaimToken;
	}
	
	public Boolean getUmaGrantAccessIfNoPolicies() {
		return umaGrantAccessIfNoPolicies;
	}
	
	public void setUmaGrantAccessIfNoPolicies(Boolean umaGrantAccessIfNoPolicies) {
		this.umaGrantAccessIfNoPolicies = umaGrantAccessIfNoPolicies;
	}
	
	public Boolean getUmaRestrictResourceToAssociatedClient() {
		return umaRestrictResourceToAssociatedClient;
	}
	
	public void setUmaRestrictResourceToAssociatedClient(Boolean umaRestrictResourceToAssociatedClient) {
		this.umaRestrictResourceToAssociatedClient = umaRestrictResourceToAssociatedClient;
	}
	
	public Boolean getUmaRptAsJwt() {
		return umaRptAsJwt;
	}
	
	public void setUmaRptAsJwt(Boolean umaRptAsJwt) {
		this.umaRptAsJwt = umaRptAsJwt;
	}
	
}
