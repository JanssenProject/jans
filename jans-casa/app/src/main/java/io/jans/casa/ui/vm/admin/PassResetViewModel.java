package io.jans.casa.ui.vm.admin;

import io.jans.casa.core.PasswordStatusService;
import io.jans.casa.misc.Utils;
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

    public boolean isPassResetEnabled() {
        return passResetEnabled;
    }

    public void setPassResetEnabled(boolean passResetEnabled) {
        this.passResetEnabled = passResetEnabled;
    }

    @Init
    public void init() {
        passResetEnabled = getSettings().isEnablePassReset();
    }

    public void change() {
        getSettings().setEnablePassReset(passResetEnabled);
        updateMainSettings();
        pst.reloadStatus();
    }

}
