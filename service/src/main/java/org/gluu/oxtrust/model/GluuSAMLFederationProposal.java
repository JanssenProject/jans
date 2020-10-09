/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.model;

import org.gluu.oxtrust.service.FederationService;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.service.cdi.util.CdiUtil;
import org.gluu.util.StringHelper;

public class GluuSAMLFederationProposal extends GluuSAMLTrustRelationship {

	private static final long serialVersionUID = 917608495756044798L;

	@AttributeName(name = "gluuRulesAccepted")
	private String gluuRulesAccepted;

	@AttributeName(name = "federationRules")
	private String federationRules;

	public void setRulesAccepted(boolean rulesAccepted) {
		this.setGluuRulesAccepted(Boolean.toString(rulesAccepted));
	}

	public boolean isRulesAccepted() {
		return StringHelper.isEmpty(getGluuRulesAccepted()) ? false : Boolean.parseBoolean(getGluuRulesAccepted());
	}

	public void setGluuRulesAccepted(String gluuRulesAccepted) {
		this.gluuRulesAccepted = gluuRulesAccepted;
	}

	public String getGluuRulesAccepted() {
		return gluuRulesAccepted;
	}

	public void setFederationRules(String federationRules) {
		this.federationRules = federationRules;
	}

	public String getFederationRules() {
		return federationRules;
	}

	public GluuSAMLFederationProposal getContainerFederation() {
		FederationService federationService = CdiUtil.bean(FederationService.class);
		return federationService.getProposalByDn(super.gluuContainerFederation);
	}

	public void setContainerFederation(GluuSAMLFederationProposal gluuContainerFederation) {
		super.gluuContainerFederation = gluuContainerFederation.getDn();
	}

}
