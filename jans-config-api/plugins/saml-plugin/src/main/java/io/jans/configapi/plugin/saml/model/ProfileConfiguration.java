/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileConfiguration implements Serializable {

    
    
	private static final String DELIMITER = ",";
	private String name;
	private boolean includeAttributeStatement;
	private String signResponses;
	private String signAssertions;
	private String signRequests;
	private int assertionLifetime;
	private int assertionProxyCount;
	private String encryptNameIds;
	private String encryptAssertions;
	private String profileConfigurationCertFileName;
	private String defaultAuthenticationMethod;
	private String nameIDFormatPrecedence;
	private List<String> nameIDFormatPrecedenceList = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isIncludeAttributeStatement() {
		return includeAttributeStatement;
	}

	public void setIncludeAttributeStatement(boolean includeAttributeStatement) {
		this.includeAttributeStatement = includeAttributeStatement;
	}

	public String getSignResponses() {
		return signResponses;
	}

	public void setSignResponses(String signResponses) {
		this.signResponses = signResponses;
	}

	public String getSignAssertions() {
		return signAssertions;
	}

	public void setSignAssertions(String signAssertions) {
		this.signAssertions = signAssertions;
	}

	public String getSignRequests() {
		return signRequests;
	}

	public void setSignRequests(String signRequests) {
		this.signRequests = signRequests;
	}

	public int getAssertionLifetime() {
		return assertionLifetime;
	}

	public void setAssertionLifetime(int assertionLifetime) {
		this.assertionLifetime = assertionLifetime;
	}

	public int getAssertionProxyCount() {
		return assertionProxyCount;
	}

	public void setAssertionProxyCount(int assertionProxyCount) {
		this.assertionProxyCount = assertionProxyCount;
	}

	public String getEncryptNameIds() {
		return encryptNameIds;
	}

	public void setEncryptNameIds(String encryptNameIds) {
		this.encryptNameIds = encryptNameIds;
	}

	public String getEncryptAssertions() {
		return encryptAssertions;
	}

	public void setEncryptAssertions(String encryptAssertions) {
		this.encryptAssertions = encryptAssertions;
	}

	public String getProfileConfigurationCertFileName() {
		return profileConfigurationCertFileName;
	}

	public void setProfileConfigurationCertFileName(String profileConfigurationCertFileName) {
		this.profileConfigurationCertFileName = profileConfigurationCertFileName;
	}

	public String getDefaultAuthenticationMethod() {
		return defaultAuthenticationMethod;
	}

	public void setDefaultAuthenticationMethod(String defaultAuthenticationMethod) {
		this.defaultAuthenticationMethod = defaultAuthenticationMethod;
	}

	public String getNameIDFormatPrecedence () {
		return nameIDFormatPrecedence ;
	}

	public void setNameIDFormatPrecedence (String nameIDFormatPrecedence) {
		this.nameIDFormatPrecedence  = nameIDFormatPrecedence ;
	}

	@Override
	public String toString() {
		return String.format(
				"ProfileConfiguration [name=%s, includeAttributeStatement=%s, signResponses=%s, signAssertions=%s, signRequests=%s, assertionLifetime=%s, assertionProxyCount=%s, encryptNameIds=%s, encryptAssertions=%s, defaultAuthenticationMethod=%s, nameIDFormatPrecedence=[%s], profileConfigurationCertFileName=%s]",
				getName(), isIncludeAttributeStatement(), getSignResponses(), getSignAssertions(), getSignRequests(),
				getAssertionLifetime(), getAssertionProxyCount(), getEncryptNameIds(), getEncryptAssertions(),
				getDefaultAuthenticationMethod(), getNameIDFormatPrecedence(), getProfileConfigurationCertFileName());
	}
	
	public void setNameIDFormatPrecedenceList(List<String> nameIDFormatPrecedenceList) {
		this.nameIDFormatPrecedenceList = nameIDFormatPrecedenceList;
		setNameIDFormatPrecedence(String.join(DELIMITER, nameIDFormatPrecedenceList));
	}
	
	public List<String> getNameIDFormatPrecedenceList() {
		if (this.nameIDFormatPrecedence != null) 
			this.nameIDFormatPrecedenceList = Arrays.asList(nameIDFormatPrecedence.split(DELIMITER));
		return nameIDFormatPrecedenceList;
	}

}
