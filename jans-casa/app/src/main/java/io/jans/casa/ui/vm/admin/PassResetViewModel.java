package io.jans.casa.ui.vm.admin;

import io.jans.casa.core.PersistenceService;
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

    private PersistenceService persistenceService;

    @WireVariable("passwordStatusService")
    private PasswordStatusService pst;

    private boolean passResetEnabled;
    private boolean passResetImpossible;

    public boolean isPassResetEnabled() {
        return passResetEnabled;
    }

    public boolean isPassResetImpossible() {
        return passResetImpossible;
    }

    public void setPassResetEnabled(boolean passResetEnabled) {
        this.passResetEnabled = passResetEnabled;
    }

    @Init
    public void init() {
        persistenceService = Utils.managedBean(PersistenceService.class);
        passResetImpossible = persistenceService.isBackendLdapEnabled();
        passResetEnabled = !passResetImpossible && getSettings().isEnablePassReset();
    }

    public void change() {
        getSettings().setEnablePassReset(passResetEnabled);
        updateMainSettings();
        pst.reloadStatus();
    }

}
