package io.jans.casa.plugins.authnmethod;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.service.SmppMobilePhoneService;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Note: No injection can take place at extensions because instances are handled by p4fj
 * @author jgomer
 * @author Stefan Andersson
 */
@Extension
public class OTPSmppExtension implements AuthnMethod {

    public static final String ACR = "smpp";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private SmppMobilePhoneService mobService;

    public OTPSmppExtension() {
        mobService = Utils.managedBean(SmppMobilePhoneService.class);
    }

    public String getUINameKey() {
        return "usr.mobile_label";
    }

    public String getAcr() {
        return ACR;
    }

    public String getPanelTitleKey() {
        return "usr.mobile_title";
    }

    public String getPanelTextKey() {
        return "usr.mobile_text";
    }

    public String getPanelButtonKey() {
        return "usr.mobile_manage";
    }

    public String getPageUrl() {
        return "/user/smpp-phone-detail.zul";
    }

    public List<BasicCredential> getEnrolledCreds(String id) {

        try {
            return mobService.getVerifiedPhones(id).stream()
                    .map(vphone -> new BasicCredential(vphone.getNickName(), vphone.getAddedOn())).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }

    }

    public int getTotalUserCreds(String id) {
        return mobService.getPhonesTotal(id);
    }

    public boolean mayBe2faActivationRequisite() {
        return Boolean.parseBoolean(mobService.getScriptPropertyValue("2fa_requisite"));
    }

    public void reloadConfiguration() {
        mobService.reloadConfiguration();
    }

}
