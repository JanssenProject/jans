package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;

import org.gluu.oxauth.model.common.GrantType;

public class DynamicRegistration  implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Boolean dynamicRegistrationEnabled;
	private Boolean dynamicRegistrationPasswordGrantTypeEnabled;
	private Boolean dynamicRegistrationPersistClientAuthorizations;
	private Boolean dynamicRegistrationScopesParamEnabled;
	private Boolean legacyDynamicRegistrationScopeParam;
	
	
	@NotBlank
	@Size(min=1)
	private String dynamicRegistrationCustomObjectClass;
	
	@Pattern(regexp ="public|pairwise")
	private String defaultSubjectType;
	
	@Min(value=0)
	@Max(value=2147483647)
	@Digits(integer = 10 , fraction = 0)
	private int dynamicRegistrationExpirationTime = -1;
	
	@NotEmpty
    @Size(min = 1)
	private Set<GrantType> dynamicGrantTypeDefault;	

	@NotNull
	@Size(min=1)
	private List<String> dynamicRegistrationCustomAttributes;
	
	private Boolean trustedClientEnabled;	
	private Boolean returnClientSecretOnRead = false;

	public Boolean getDynamicRegistrationEnabled() {
		return dynamicRegistrationEnabled;
	}

	public void setDynamicRegistrationEnabled(Boolean dynamicRegistrationEnabled) {
		this.dynamicRegistrationEnabled = dynamicRegistrationEnabled;
	}

	public Boolean getDynamicRegistrationPasswordGrantTypeEnabled() {
		return dynamicRegistrationPasswordGrantTypeEnabled;
	}

	public void setDynamicRegistrationPasswordGrantTypeEnabled(Boolean dynamicRegistrationPasswordGrantTypeEnabled) {
		this.dynamicRegistrationPasswordGrantTypeEnabled = dynamicRegistrationPasswordGrantTypeEnabled;
	}

	public Boolean getDynamicRegistrationPersistClientAuthorizations() {
		return dynamicRegistrationPersistClientAuthorizations;
	}

	public void setDynamicRegistrationPersistClientAuthorizations(Boolean dynamicRegistrationPersistClientAuthorizations) {
		this.dynamicRegistrationPersistClientAuthorizations = dynamicRegistrationPersistClientAuthorizations;
	}

	public Boolean getDynamicRegistrationScopesParamEnabled() {
		return dynamicRegistrationScopesParamEnabled;
	}

	public void setDynamicRegistrationScopesParamEnabled(Boolean dynamicRegistrationScopesParamEnabled) {
		this.dynamicRegistrationScopesParamEnabled = dynamicRegistrationScopesParamEnabled;
	}

	public Boolean getLegacyDynamicRegistrationScopeParam() {
		return legacyDynamicRegistrationScopeParam;
	}

	public void setLegacyDynamicRegistrationScopeParam(Boolean legacyDynamicRegistrationScopeParam) {
		this.legacyDynamicRegistrationScopeParam = legacyDynamicRegistrationScopeParam;
	}

	public String getDynamicRegistrationCustomObjectClass() {
		return dynamicRegistrationCustomObjectClass;
	}

	public void setDynamicRegistrationCustomObjectClass(String dynamicRegistrationCustomObjectClass) {
		this.dynamicRegistrationCustomObjectClass = dynamicRegistrationCustomObjectClass;
	}

	public String getDefaultSubjectType() {
		return defaultSubjectType;
	}

	public void setDefaultSubjectType(String defaultSubjectType) {
		this.defaultSubjectType = defaultSubjectType;
	}

	public int getDynamicRegistrationExpirationTime() {
		return dynamicRegistrationExpirationTime;
	}

	public void setDynamicRegistrationExpirationTime(int dynamicRegistrationExpirationTime) {
		this.dynamicRegistrationExpirationTime = dynamicRegistrationExpirationTime;
	}

	public Set<GrantType> getDynamicGrantTypeDefault() {
		return dynamicGrantTypeDefault;
	}
	
	public void setDynamicGrantTypeDefault(Set<GrantType> dynamicGrantTypeDefault) {
		this.dynamicGrantTypeDefault = dynamicGrantTypeDefault;
	}

	public List<String> getDynamicRegistrationCustomAttributes() {
		return dynamicRegistrationCustomAttributes;
	}

	public void setDynamicRegistrationCustomAttributes(List<String> dynamicRegistrationCustomAttributes) {
		this.dynamicRegistrationCustomAttributes = dynamicRegistrationCustomAttributes;
	}

	public Boolean getTrustedClientEnabled() {
		return trustedClientEnabled;
	}

	public void setTrustedClientEnabled(Boolean trustedClientEnabled) {
		this.trustedClientEnabled = trustedClientEnabled;
	}

	public Boolean getReturnClientSecretOnRead() {
		return returnClientSecretOnRead;
	}

	public void setReturnClientSecretOnRead(Boolean returnClientSecretOnRead) {
		this.returnClientSecretOnRead = returnClientSecretOnRead;
	}
	
}
