package io.jans.casa.plugins.credentials.extensions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.bioid.BioIDService;
import io.jans.casa.service.ISessionContext;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;

/**
 * @author madhumita
 *
 */
@Extension
public class BioidExtension implements AuthnMethod {

	private BioIDService bioidService;
	

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ISessionContext sessionContext;

	public BioidExtension() {
		sessionContext = Utils.managedBean(ISessionContext.class);
		bioidService = BioIDService.getInstance();
	}

	public String getUINameKey() {

		return "bioid_label";
	}

	public String getAcr() {
		return BioIDService.getInstance().ACR;
	}

	public String getPanelTitleKey() {
		return "bioid_title";
	}

	public String getPanelTextKey() {
		return "bioid_text";
	}

	public String getPanelButtonKey() {

		return "bioid_manage";
	}

	public String getPanelBottomTextKey() {
		return "bioid_download";
	}

	public String getPageUrl() {
		return "user/cred_details.zul";

	}

	public List<BasicCredential> getEnrolledCreds(String id) {
		//pass user name or anything that uniquely identifies a user 
		String userName = sessionContext.getLoggedUser().getUserName();

		try {
			return BioIDService.getInstance().getBioIDDevices(sessionContext.getLoggedUser().getId()).stream()
					.map(dev -> new BasicCredential(Labels.getLabel(BioIDService.getInstance().TRAIT_LABEL_FACE_PERIOCULAR) , dev.getAddedOn())).collect(Collectors.toList());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyList();
		}

	}

	public int getTotalUserCreds(String id) {
		//pass user name or anything that uniquely identifies a user 
		String userName = sessionContext.getLoggedUser().getUserName();
		return BioIDService.getInstance().getDeviceTotal(sessionContext.getLoggedUser().getId());
	}

	public void reloadConfiguration() {
		BioIDService.getInstance().reloadConfiguration();

	}

	public boolean mayBe2faActivationRequisite() {
		return Boolean.parseBoolean(Optional
				.ofNullable(BioIDService.getInstance().getScriptPropertyValue("2fa_requisite")).orElse("false"));
	}
	
}
