package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class SubjectConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotEmpty
    @Size(min = 1)
	private List<String> subjectTypesSupported;
	private Boolean shareSubjectIdBetweenClientsWithSameSectorId = false;
	
	
	public List<String> getSubjectTypesSupported() {
		return subjectTypesSupported;
	}
	
	public void setSubjectTypesSupported(List<String> subjectTypesSupported) {
		this.subjectTypesSupported = subjectTypesSupported;
	}
	
	public Boolean getShareSubjectIdBetweenClientsWithSameSectorId() {
		return shareSubjectIdBetweenClientsWithSameSectorId;
	}
	
	public void setShareSubjectIdBetweenClientsWithSameSectorId(Boolean shareSubjectIdBetweenClientsWithSameSectorId) {
		this.shareSubjectIdBetweenClientsWithSameSectorId = shareSubjectIdBetweenClientsWithSameSectorId;
	}
}
