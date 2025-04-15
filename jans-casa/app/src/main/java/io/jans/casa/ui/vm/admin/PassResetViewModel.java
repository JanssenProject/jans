package io.jans.casa.ui.vm.admin;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.PasswordStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.select.annotation.WireVariable;

/**
 * @author jgomer
 */
public class PassResetViewModel extends MainViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable("passwordStatusService")
    private PasswordStatusService pst;

    private boolean passResetEnabled;
    private boolean passPolicy;

    public boolean isPassResetEnabled() {
        return passResetEnabled;
    }

    public void setPassResetEnabled(boolean passResetEnabled) {
        this.passResetEnabled = passResetEnabled;
    }

    public boolean isPassPolicy() {
        return passPolicy;
    }

    public void setPassPolicy(boolean passPolicy) {
        this.passPolicy = passPolicy;
    }

    @Init
    public void init() {
        MainSettings ms = getSettings();
        passResetEnabled = ms.isEnablePassReset();
        passPolicy = ms.isUsePasswordPolicy();        
    }

    public void update() {
        
        MainSettings ms = getSettings();
        ms.setEnablePassReset(passResetEnabled);
        ms.setUsePasswordPolicy(passPolicy);
        
        updateMainSettings();
        pst.reloadStatus();

    }

}
