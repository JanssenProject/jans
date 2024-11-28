package io.jans.casa.plugins.emailotp.extension;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.plugins.emailotp.service.EmailOtpService;

import java.util.*;
import java.util.stream.Collectors;

import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class EmailOtpAuthnMethod implements AuthnMethod {

	private EmailOtpService mailOtpService;
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	public EmailOtpAuthnMethod() {
		mailOtpService = EmailOtpService.getInstance();
	}

	public String getUINameKey() {
		return "email2fa.method_label";
	}

	public String getPanelTitleKey() {
		return "email2fa.method_title";
	}

	public String getPanelTextKey() {
		return "email2fa.method_text";
	}

	public String getPanelButtonKey() {
		return "email2fa.method_button_label";
	}

	public String getPageUrl() {
		return "emails.zul";
	}

	public String getAcr() {
	    return EmailOtpService.AGAMA_FLOW;
	}

	public void reloadConfiguration() {
	}

	public List<BasicCredential> getEnrolledCreds(String id) {
	    return mailOtpService.emailsOf(id).stream().map(e -> new BasicCredential(e, 0L))
	            .collect(Collectors.toList());
	}

	public int getTotalUserCreds(String id) {
	    return getEnrolledCreds(id).size();
	}

}
