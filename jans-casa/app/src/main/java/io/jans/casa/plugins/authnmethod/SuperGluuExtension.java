package io.jans.casa.plugins.authnmethod;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.service.SGService;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jgomer
 */
@Extension
public class SuperGluuExtension implements AuthnMethod {

    public static final String ACR = "super_gluu";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private SGService sgService;

    public SuperGluuExtension() {
        sgService = Utils.managedBean(SGService.class);
    }

    public String getUINameKey() {
        return "usr.supergluu_label";
    }

    public String getAcr() {
        return ACR;
    }

    public String getPanelTitleKey() {
        return "usr.supergluu_title";
    }

    public String getPanelTextKey() {
        return "usr.supergluu_text";
    }

    public String getPanelButtonKey() {
        return "usr.supergluu_manage";
    }

    public String getPanelBottomTextKey() {
        return "usr.supergluu_download";
    }

    public String getPageUrl() {
        return "/user/super-detail.zul";
    }

    public List<BasicCredential> getEnrolledCreds(String id) {

        try {
            return sgService.getDevices(id, true).stream()
                    .map(dev -> new BasicCredential(dev.getNickName(), dev.getCreationDate().getTime())).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public int getTotalUserCreds(String id) {
        return sgService.getDevicesTotal(id, true);
    }

    public void reloadConfiguration() {
        sgService.reloadConfiguration();
    }

    public boolean mayBe2faActivationRequisite() {
        return Boolean.parseBoolean(sgService.getScriptPropertyValue("2fa_requisite"));
    }

}
