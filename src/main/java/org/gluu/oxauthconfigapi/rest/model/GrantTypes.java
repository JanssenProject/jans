package org.gluu.oxauthconfigapi.rest.model;


import java.io.Serializable;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import org.gluu.oxauth.model.common.GrantType;

public class GrantTypes implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
    @NotEmpty
    @Size(min = 1)
	private Set<GrantType> grantTypesSupported;
    
    @NotEmpty
    @Size(min = 1)
	private Set<GrantType> dynamicGrantTypeDefault;
	
	public Set<GrantType> getGrantTypesSupported() {
		return grantTypesSupported;
	}
	
	public void setGrantTypesSupported(Set<GrantType> grantTypesSupported) {
		this.grantTypesSupported = grantTypesSupported;
	}
	
	public Set<GrantType> getDynamicGrantTypeDefault() {
		return dynamicGrantTypeDefault;
	}
	
	public void setDynamicGrantTypeDefault(Set<GrantType> dynamicGrantTypeDefault) {
		this.dynamicGrantTypeDefault = dynamicGrantTypeDefault;
	}
	
}
