package io.jans.casa.plugins.credentials.extensions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.sample.SampleService;
import io.jans.casa.service.ISessionContext;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author madhumita
 *
 */
@Extension
public class SampleExtension implements AuthnMethod {

	private SampleService sampleService;

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ISessionContext sessionContext;

	public SampleExtension() {
		sessionContext = Utils.managedBean(ISessionContext.class);
		sampleService = SampleService.getInstance();
	}

	public String getUINameKey() {

		return "sample_label";
	}

	public String getAcr() {
		return sampleService.getInstance().ACR;
	}

	public String getPanelTitleKey() {
		return "sample_title";
	}

	public String getPanelTextKey() {
		return "sample_text";
	}

	public String getPanelButtonKey() {

		return "sample_manage";
	}

	public String getPanelBottomTextKey() {
		return "sample_download";
	}

	public String getPageUrl() {
		return "user/cred_details.zul";

	}

	public List<BasicCredential> getEnrolledCreds(String id) {
		//pass user name or anything that uniquely identifies a user 
		String userName = sessionContext.getLoggedUser().getUserName();

		try {
			return sampleService.getInstance().getDevices(userName).stream()
					.map(dev -> new BasicCredential(dev.getNickName(), 0)).collect(Collectors.toList());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyList();
		}

	}

	public int getTotalUserCreds(String id) {
		//pass user name or anything that uniquely identifies a user 
		String userName = sessionContext.getLoggedUser().getUserName();
		return sampleService.getInstance().getDeviceTotal(userName);
	}

	public void reloadConfiguration() {
		SampleService.getInstance().reloadConfiguration();

	}

	public boolean mayBe2faActivationRequisite() {
		return Boolean.parseBoolean(Optional
				.ofNullable(SampleService.getInstance().getScriptPropertyValue("2fa_requisite")).orElse("false"));
	}
	
}
