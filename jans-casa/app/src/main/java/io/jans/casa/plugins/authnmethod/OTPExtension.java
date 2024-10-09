package io.jans.casa.plugins.authnmethod;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.service.OTPService;

import java.util.*;
import java.util.stream.Collectors;

import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note: No injection can take place at extensions because instances are handled by p4fj
 */
@Extension
public class OTPExtension implements AuthnMethod {

    public static final String ACR = "io.jans.casa.authn.otp";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private OTPService otpService;

    public OTPExtension() {
        otpService = Utils.managedBean(OTPService.class);
    }

    public String getUINameKey() {
        return "usr.otp_label";
    }

    public String getAcr() {
        return ACR;
    }

    public String getPanelTitleKey() {
        return "usr.otp_title";
    }

    public String getPanelTextKey() {
        return "usr.otp_text";
    }

    public String getPanelButtonKey() {
        return "usr.otp_manage";
    }

    public String getPanelBottomTextKey() {
        return "usr.otp_gauth_download";
    }

    public String getPageUrl() {
        return "/user/otp-detail.zul";
    }

    public List<BasicCredential> getEnrolledCreds(String id) {

        try {
            return otpService.getDevices(id).stream()
                    .map(dev -> new BasicCredential(dev.getNickName(), dev.getAddedOn())).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public int getTotalUserCreds(String id) {
        return otpService.getDevicesTotal(id);
    }

    public boolean mayBe2faActivationRequisite() {
        return Boolean.parseBoolean(otpService.getPropertyValue("2fa_requisite"));
    }

    public void reloadConfiguration() {
        otpService.reloadConfiguration();
    }

}
