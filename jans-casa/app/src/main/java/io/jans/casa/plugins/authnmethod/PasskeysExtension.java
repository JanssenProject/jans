package io.jans.casa.plugins.authnmethod;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.service.Fido2Service;

import java.util.*;
import java.util.stream.Collectors;

import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class PasskeysExtension implements AuthnMethod {

    public static final String ACR = "io.jans.casa.authn.fido2";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Fido2Service fido2Service;

    public PasskeysExtension() {
        fido2Service = Utils.managedBean(Fido2Service.class);
    }

    public String getUINameKey() {
        return "usr.fido2_label";
    }

    public String getAcr() {
        return ACR;
    }

    public String getPanelTitleKey() {
        return "usr.fido2_title";
    }

    public String getPanelTextKey() {
        return "usr.fido2_text";
    }

    public String getPanelButtonKey() {
        return "usr.fido2_manage";
    }

    public String getPanelBottomTextKey() {
        return "usr.fido2_buy_title";
    }

    public String getPageUrl() {
        return "/user/fido2-detail.zul";
    }

    public List<BasicCredential> getEnrolledCreds(String id) {

        try {
            return fido2Service.getDevices(id, fido2Service.appId(), true).stream()
                    .map(dev -> new BasicCredential(dev.getNickName(), dev.getCreationDate().getTime())).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public int getTotalUserCreds(String id) {
		try {
			return fido2Service.getDevicesTotal(id, fido2Service.appId(), true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return 0;
		}
    }

    public boolean mayBe2faActivationRequisite() {
        return Boolean.parseBoolean(fido2Service.getPropertyValue("2fa_requisite"));
    }

    public void reloadConfiguration() {
        fido2Service.reloadConfiguration();
    }

}
