package io.jans.casa.ui.vm.user;

import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.service.IPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.resource.Labels;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the ViewModel of page user.zul (the main page of this app). Main
 * functionalities controlled here are: summary of users's enrolled devices by
 * type
 * 
 * @author jgomer
 */
public class UserMainViewModel extends UserViewModel {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String introText;
	private boolean methodsAvailability;
	private boolean has2faRequisites;

	private List<AuthnMethod> widgets;
	private List<AuthnMethod> pre2faMethods;

	public String getIntroText() {
		return introText;
	}

	public boolean isMethodsAvailability() {
		return methodsAvailability;
	}

	public boolean isHas2faRequisites() {
		return has2faRequisites;
	}

	public List<AuthnMethod> getWidgets() {
		return widgets;
	}

	public List<AuthnMethod> getPre2faMethods() {
		return pre2faMethods; 
	}

	@Init(superclass = true)
	public void childInit() {

		widgets = userService.getLiveAuthnMethods();
		methodsAvailability = widgets.size() > 0;
		pre2faMethods = new ArrayList<>();

		if (methodsAvailability) {
			StringBuffer helper = new StringBuffer();
			widgets.forEach(aMethod -> helper.append(", ").append(Labels.getLabel(aMethod.getPanelTitleKey())));
			String orgName = Utils.managedBean(IPersistenceService.class).getOrganization().getDisplayName();
			introText = Labels.getLabel("usr.main_intro", new String[] { orgName, helper.substring(2) });

			pre2faMethods = widgets.stream().filter(AuthnMethod::mayBe2faActivationRequisite).collect(Collectors.toList());
			has2faRequisites = pre2faMethods.size() == 0 
					|| pre2faMethods.stream().anyMatch(aMethod -> aMethod.getTotalUserCreds(user.getId()) > 0);
		}

	}

}
