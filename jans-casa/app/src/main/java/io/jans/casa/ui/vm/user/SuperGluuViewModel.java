package io.jans.casa.ui.vm.user;

import io.jans.casa.misc.WebUtils;
import io.jans.casa.core.pojo.SuperGluuDevice;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.SuperGluuExtension;
import io.jans.casa.plugins.authnmethod.conf.SGConfig;
import io.jans.casa.plugins.authnmethod.service.SGService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.json.JavaScriptValue;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Messagebox;

import java.util.List;

/**
 * Created by jgomer on 2017-09-06.
 * This is the ViewModel of page super-detail.zul. It controls the CRUD of supergluu devices
 */
public class SuperGluuViewModel extends UserViewModel {

    private static final int QR_SCAN_TIMEOUT = 60;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable("sGService")
    private SGService sgService;

    private SGConfig sgConfig;
    private SuperGluuDevice newDevice;
    private String editingId;
    private List<SuperGluuDevice> devices;

    private boolean uiQRShown;
    private boolean uiEnrolled;

    public boolean isUiQRShown() {
        return uiQRShown;
    }

    public boolean isUiEnrolled() {
        return uiEnrolled;
    }

    public List<SuperGluuDevice> getDevices() {
        return devices;
    }

    public SuperGluuDevice getNewDevice() {
        return newDevice;
    }

    public void setNewDevice(SuperGluuDevice newDevice) {
        this.newDevice = newDevice;
    }

    public String getEditingId() {
        return editingId;
    }

    public void setEditingId(String editingId) {
        this.editingId = editingId;
    }

    @Init(superclass = true)
    public void childInit() throws Exception {
        devices = sgService.getDevices(user.getId(), true);
        sgConfig = sgService.getConf();

        newDevice = new SuperGluuDevice();
    }

    public void showQR() {

        try {
            
            String code = WebUtils.getValueFromCookie("session_id"); //userService.generateRandEnrollmentCode(user.getId());
            
            String request = sgService.generateRequest(user.getUserName(), code, WebUtils.getRemoteIP());

            if (request != null) {
                uiQRShown = true;
                BindUtils.postNotifyChange(this, "uiQRShown");

                //Passing screen width as max allowed size for QR code allows showing QRs even in small mobile devices
                JavaScriptValue jvalue = new JavaScriptValue(sgConfig.getFormattedQROptions(getScreenWidth()));
                //Calls the startQR javascript function supplying suitable params
                Clients.response(new AuInvoke("startQR", request, sgConfig.getLabel(), jvalue, QR_SCAN_TIMEOUT, true));
            } else {
                UIUtils.showMessageUI(false);
            }
        } catch (Exception e) {
            UIUtils.showMessageUI(false);
            logger.error(e.getMessage(), e);
        }

    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view){
        Selectors.wireEventListeners(view, this);
    }

    private void stopPolling(){
        Clients.response(new AuInvoke("stopPolling"));
        uiQRShown = false;
        BindUtils.postNotifyChange(this, "uiQRShown");
        userService.cleanRandEnrollmentCode(user.getId());
    }

    @Listen("onData=#readyButton")
    public void qrScanResult(Event event) {

        if (uiQRShown) {
            switch (event.getData().toString()) {
                case "timeout":
                    //Indicates progress bar reached 100%
                    stopPolling();
                    break;
                case "poll":
                	
                    newDevice = sgService.getLatestSuperGluuDevice(user.getId(), sgService.getConf().getAppId());
                    
                    if (newDevice != null) {    //New device detected, stop polling
                        stopPolling();
                        try {
                            logger.debug("qrScanResult. Got device {}", newDevice.getId());
                            //It's enrolled in LDAP, nonetheless we are missing the nickname yet and also the check if
                            //it has not previously been enrolled by the same user
                            uiEnrolled = sgService.isDeviceUnique(newDevice, user.getId());
                            if (uiEnrolled) {
                                BindUtils.postNotifyChange(this, "uiEnrolled");
                            } else {
                                //drop duplicated device from LDAP
                                sgService.removeDevice(newDevice,user.getId());
                                logger.info("Duplicated SuperGluu device {} has been removed", newDevice.getDeviceData().getUuid());
                                UIUtils.showMessageUI(false, Labels.getLabel("usr.supergluu_already_enrolled"));
                            }
                        } catch (Exception e) {
                            String error = e.getMessage();
                            logger.error(error, e);
                            UIUtils.showMessageUI(false, Labels.getLabel("general.error.detailed", new String[] { error }));
                        }
                    }
                    break;
                default:
                    //Added to pass checkstyler
            }
        }

    }

    @NotifyChange({"uiQRShown", "uiEnrolled", "newDevice", "devices"})
    public void add() {

        if (Utils.isNotEmpty(newDevice.getNickName())) {
            try {
                sgService.updateDevice(newDevice, user.getId());
                devices.add(newDevice);
                UIUtils.showMessageUI(true, Labels.getLabel("usr.enroll.success"));
                userService.notifyEnrollment(user, SuperGluuExtension.ACR);
            } catch (Exception e) {
                UIUtils.showMessageUI(false, Labels.getLabel("usr.error_updating"));
                logger.error(e.getMessage(), e);
            }
            resetAddSettings();
        }

    }

    @NotifyChange({"uiQRShown", "uiEnrolled", "newDevice"})
    public void cancel() {
        try {
            //Stop tellServer function if still running
            Clients.response(new AuInvoke("stopPolling"));
            //Check if cancelation was made after a real enrollment took place
            if (newDevice != null && newDevice.getDeviceData() != null) {
                sgService.removeDevice(newDevice, user.getId());
            }
        } catch (Exception e) {
            UIUtils.showMessageUI(false);
            logger.error(e.getMessage(), e);
        }
        resetAddSettings();

    }

    @NotifyChange({"editingId", "newDevice"})
    public void prepareForUpdate(SuperGluuDevice dev) {
        //This will make the modal window to become visible
        editingId = dev.getId();
        newDevice = new SuperGluuDevice();
        newDevice.setNickName(dev.getNickName());
    }

    @NotifyChange({"devices", "editingId", "newDevice"})
    public void update() {

        String nick = newDevice.getNickName();
        if (Utils.isNotEmpty(nick)) {
            int i = Utils.firstTrue(devices, dev -> dev.getId().equals(editingId));
            SuperGluuDevice dev = devices.get(i);
            dev.setNickName(nick);
            cancelUpdate(null);

            try {
                sgService.updateDevice(dev, user.getId());
                UIUtils.showMessageUI(true);
            } catch (Exception e) {
                UIUtils.showMessageUI(false);
                logger.error(e.getMessage(), e);
            }
        }

    }

    @NotifyChange({"editingId", "newDevice"})
    public void cancelUpdate(Event event) {
        newDevice.setNickName(null);
        editingId = null;
        if (event != null && event.getName().equals(Events.ON_CLOSE)) {
            event.stopPropagation();
        }
    }

    public void delete(SuperGluuDevice device){

        String resetMessages = resetPreferenceMessage(SuperGluuExtension.ACR, devices.size());
        boolean reset = resetMessages != null;
        Pair<String, String> delMessages = getDeleteMessages(device.getNickName(), resetMessages);

        Messagebox.show(delMessages.getY(), delMessages.getX(), Messagebox.YES | Messagebox.NO,
                reset ? Messagebox.EXCLAMATION : Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        try {
                            devices.remove(device);
                            boolean success = sgService.removeDevice(device, user.getId());

                            if (success) {
                                if (reset) {
                                    userService.turn2faOff(user);
                                }
                                //trigger refresh (this method is asynchronous...)
                                BindUtils.postNotifyChange(SuperGluuViewModel.this, "devices");
                            } else {
                                devices.add(device);
                            }
                            UIUtils.showMessageUI(success);
                        } catch (Exception e) {
                            UIUtils.showMessageUI(false);
                            logger.error(e.getMessage(), e);
                        }
                    }
                });
    }

    private void resetAddSettings() {
        uiQRShown = false;
        uiEnrolled = false;
        newDevice = new SuperGluuDevice();
        userService.cleanRandEnrollmentCode(user.getId());
    }

}
