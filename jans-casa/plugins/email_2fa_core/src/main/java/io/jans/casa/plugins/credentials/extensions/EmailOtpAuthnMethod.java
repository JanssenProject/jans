package io.jans.casa.plugins.credentials.extensions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.emailotp.EmailOtpService;
import io.jans.casa.service.ISessionContext;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class EmailOtpAuthnMethod implements AuthnMethod {

    private static Logger logger = LoggerFactory.getLogger(EmailOtpAuthnMethod.class);

    private ISessionContext sessionContext;

    public EmailOtpAuthnMethod() {
        sessionContext = Utils.managedBean(ISessionContext.class);
        reloadConfiguration();
    }

    @Override
    public String getPanelBottomTextKey() {
        return "";
    }

    @Override
    public boolean mayBe2faActivationRequisite() {
        return Boolean.parseBoolean(Optional
                .ofNullable(EmailOtpService.getInstance().getScriptPropertyValue("2fa_requisite")).orElse("false"));

    }

    @Override
    public String getAcr() {
        return EmailOtpService.ACR;
    }

    @Override
    public List<BasicCredential> getEnrolledCreds(String arg0) {
        try {
            return EmailOtpService.getInstance().getCredentials(sessionContext.getLoggedUser().getId())
                    .stream().map(dev -> new BasicCredential(dev.getNickName(), 0)).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getPageUrl() {
        return "user/cred_details.zul";
    }

    @Override
    public String getPanelButtonKey() {
        return "panel.button";
    }

    @Override
    public String getPanelTextKey() {
        return "panel.text";
    }

    @Override
    public String getPanelTitleKey() {
        return "email.title";
    }

    @Override
    public int getTotalUserCreds(String arg0) {
        return EmailOtpService.getInstance().getCredentialsTotal( sessionContext.getLoggedUser().getId());
    }

    @Override
    public String getUINameKey() {
        return "email.title";
    }

    @Override
    public void reloadConfiguration() {
        EmailOtpService.getInstance().reloadConfiguration();
    }

}
