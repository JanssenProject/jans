package io.jans.casa.ui.vm.user;

import io.jans.casa.core.PasswordStatusService;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.misc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zul.Messagebox;

/**
 * Created by jgomer on 2018-07-09.
 */
public class PassResetViewModel extends UserViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private PasswordStatusService pst;
    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirm;
    private int strength;

    @DependsOn("strength")
    public String getStrengthText() {
        String str = null;
        if (strength >= 0) {
            str = Labels.getLabel("usr.pass.strength.level." + strength);
        }
        return str;
    }

    public int getStrength() {
        return strength;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirm() {
        return newPasswordConfirm;
    }

    public void setNewPasswordConfirm(String newPasswordConfirm) {
        this.newPasswordConfirm = newPasswordConfirm;
    }

    @Init(superclass = true)
    public void childInit() {
        resetPassSettings();
        pst = getPst();
    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
        Selectors.wireEventListeners(view, this);
    }

    @Listen("onData=#new_pass")
    public void notified(Event event) throws Exception {
        if (Utils.isNotEmpty(newPassword)) {
            strength = (int) event.getData();
        } else {
            strength = -1;
        }
        BindUtils.postNotifyChange(this, "strength");
    }

    @NotifyChange("*")
    public void resetPass() {

        if (pst.passwordMatch(user.getUserName(), currentPassword)) {
            if (newPasswordConfirm != null && newPasswordConfirm.equals(newPassword)) {
                Boolean success = pst.changePassword(user.getId(), newPassword);

                if (success == null) {
                    UIUtils.showMessageUI(false, Labels.getLabel("usr.pass.validation_failed"));
                } else if (!success) {
                    UIUtils.showMessageUI(false);
                } else {
                    logger.info("User {} has changed his password", user.getUserName());
                    resetPassSettings();
                    UIUtils.showMessageUI(true, Labels.getLabel("usr.passreset_changed"));
                }

            } else {
                UIUtils.showMessageUI(false, Labels.getLabel("usr.passreset_nomatch"));
                String tmp = currentPassword;
                resetPassSettings();
                currentPassword = tmp;
            }
        } else {
            currentPassword = null;
            UIUtils.showMessageUI(false, Labels.getLabel("usr.passreset_badoldpass"));
        }

    }

    @NotifyChange("*")
    public void setPass() {

        if ((newPasswordConfirm != null && newPasswordConfirm.equals(newPassword))) {

            Boolean success = pst.changePassword(user.getId(), newPassword); 
            if (success == null) {
                UIUtils.showMessageUI(false, Labels.getLabel("usr.pass.validation_failed"));
            } else if (!success) {
                UIUtils.showMessageUI(false);
            } else {            
                String userName = user.getUserName();
                logger.info("User {} assigned his password", userName);

                resetPassSettings();
                pst.reloadStatus();
                Messagebox.show(Labels.getLabel("usr.password_set.success", new String[]{userName}), null, Messagebox.OK, Messagebox.INFORMATION);
            }
        } else {
            resetPassSettings();
            UIUtils.showMessageUI(false, Labels.getLabel("usr.passreset_nomatch"));
        }

    }

    @NotifyChange("*")
    public void cancel() {
        resetPassSettings();
    }

    private void resetPassSettings() {
        newPassword = null;
        newPasswordConfirm = null;
        currentPassword = null;
        strength = -1;
    }

}
