package io.jans.casa.ui.vm.user;

import io.jans.casa.core.pojo.OTPDevice;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.OTPExtension;
import io.jans.casa.plugins.authnmethod.conf.OTPConfig;
import io.jans.casa.plugins.authnmethod.service.OTPService;
import io.jans.casa.plugins.authnmethod.service.otp.IOTPAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.json.JavaScriptValue;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Messagebox;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.lochbridge.oath.otp.keyprovisioning.OTPKey.OTPType;

/**
 * This is the ViewModel of page otp-detail.zul. It controls the CRUD of HOTP/TOTP devices
 * @author jgomer
 */
public class OTPViewModel extends UserViewModel {

    private static final int QR_SCAN_TIMEOUT = 60;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable("oTPService")
    private OTPService otpService;

    private String code;
    private List<OTPDevice> devices;
    private OTPDevice newDevice;
    private int editingId;
    private String secretKeyString;
    private byte[] secretKey;
    private OTPConfig otpConfig;

    private String tokenType;
    private OTPType hardTokenType;

    private boolean uiQRShown;
    private boolean uiCorrectCode;
    private boolean uiTokenPressing;

    public boolean isUiCorrectCode() {
        return uiCorrectCode;
    }

    public boolean isUiQRShown() {
        return uiQRShown;
    }

    public boolean isUiTokenPressing() {
        return uiTokenPressing;
    }

    public int getEditingId() {
        return editingId;
    }

    @DependsOn({"tokenType", "hardTokenType"})
    public int getDigitLength() {

        int len;
        switch (getApplicableAlgorithmType()) {
            case TOTP:
                len = otpConfig.getTotp().getDigits();
                break;
            case HOTP:
                len = otpConfig.getHotp().getDigits();
                break;
            default:
                len = 0;
        }
        return len;

    }

    public String getTokenType() {
        return tokenType;
    }

    public OTPType getHardTokenType() {
        return hardTokenType;
    }

    public String getCode() {
        return code;
    }

    public OTPDevice getNewDevice() {
        return newDevice;
    }

    public List<OTPDevice> getDevices() {
        return devices;
    }

    public String getSecretKeyString() {
        return secretKeyString;
    }

    public void setSecretKeyString(String secretKeyString) {
        this.secretKeyString = secretKeyString;
    }

    public void setNewDevice(OTPDevice newDevice) {
        this.newDevice = newDevice;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private OTPType getApplicableAlgorithmType() {
        //Assume TOTP if no selections have been made
        return tokenType != null && tokenType.equals("HARD") && hardTokenType != null
                && hardTokenType.equals(OTPType.HOTP) ? OTPType.HOTP : OTPType.TOTP;
    }

    @Init(superclass = true)
    public void childInit() throws Exception {
        newDevice = new OTPDevice();
        devices = otpService.getDevices(user.getId());
        otpConfig = otpService.getConf();
    }

    @NotifyChange("*")
    public void chooseType(String type, HtmlBasedComponent comp) {
        cancel();
        tokenType = type;
        focus(comp);
    }

    @NotifyChange({"uiQRShown", "uiCorrectCode"})
    public void showQR(HtmlBasedComponent comp) {

        uiQRShown = true;
        uiCorrectCode = false;
        code = null;

        //For QR scan TOTP is used
        IOTPAlgorithm totpService = otpService.getAlgorithmService(OTPType.TOTP);
        secretKey = totpService.generateSecretKey();
        String label = Optional.ofNullable(user.getGivenName()).orElse(user.getUserName());
        String request = totpService.generateSecretKeyUri(secretKey, label);

        JavaScriptValue jvalue = new JavaScriptValue(otpConfig.getFormattedQROptions(getScreenWidth()));

        //Calls the startQR javascript function supplying suitable params
        Clients.response(new AuInvoke("startQR", request, otpConfig.getLabel(), jvalue, QR_SCAN_TIMEOUT));
        //Scroll down a little bit so content below QR code is noticeable
        Clients.scrollBy(0, 10);
        focus(comp);

    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
        Selectors.wireEventListeners(view, this);
    }

    @Listen("onData=#readyButton")
    public void timedOut(Event event) throws Exception {
        if (uiQRShown) {
            //Restore UI because user did not scan code
            uiQRShown = false;
            BindUtils.postNotifyChange(this, "uiQRShown");
        }
    }

    @NotifyChange("*")
    public void cancel() {

        if (uiQRShown) {
            Clients.response(new AuInvoke("clean"));
            uiQRShown = false;
        }
        uiCorrectCode = false;
        uiTokenPressing = false;
        code = null;
        tokenType = null;
        secretKeyString = null;
        hardTokenType = null;
        newDevice = new OTPDevice();

    }

    @NotifyChange({"hardTokenType"})
    public void changeHardType(boolean timeBased) {
        hardTokenType = timeBased ? OTPType.TOTP : OTPType.HOTP;
    }

    @NotifyChange({"uiTokenPressing"})
    public void changeTokenPressing(HtmlBasedComponent comp) {

        //Generate the binary key based on user input
        secretKey = getSecretKeyFrom(secretKeyString);
        if (secretKey != null) {
            if (comp != null) {
                comp.focus();
            }
            uiTokenPressing = !uiTokenPressing;
        }

    }

    @NotifyChange("uiCorrectCode")
    public void validateCode() {

        String uid = null;
        if (code != null) {
            logger.trace("Validating code entered");

            if (tokenType.equals("HARD")) {
                //User may have retyped the key without pressing the "continue" button
                secretKey = getSecretKeyFrom(secretKeyString);
                if (secretKey == null) {
                    return;
                }
            }
            //Determines if numeric code is valid with respect to secret key
            IOTPAlgorithm service = otpService.getAlgorithmService(getApplicableAlgorithmType());
            uid = service.getExternalUid(Base64.getUrlEncoder().encodeToString(secretKey), code);
            if (uid != null) {

                //User may have entered the same key manually in the past
                int semicolon = uid.indexOf(";");
                final String shorterUid = semicolon == -1 ? uid : uid.substring(0, semicolon);

                if (devices.stream().anyMatch(dev -> dev.getUid().startsWith(shorterUid))) {
                    UIUtils.showMessageUI(false, Labels.getLabel("usr.otp_duplicated_device"));
                } else {
                    newDevice.setUid(uid);
                    uiCorrectCode = true;
                }
            }
        }
        if (uid == null) {
            UIUtils.showMessageUI(Clients.NOTIFICATION_TYPE_WARNING, Labels.getLabel("usr.code_wrong"));
        }

    }

    @NotifyChange("*")
    public void add() {

        //Adds the new device if user typed a nickname in the text box
        if (Utils.isNotEmpty(newDevice.getNickName())) {
            if (enroll()) {
                UIUtils.showMessageUI(true, Labels.getLabel("usr.enroll.success"));
                cancel();
                userService.notifyEnrollment(user, OTPExtension.ACR);
            } else {
                UIUtils.showMessageUI(false, Labels.getLabel("usr.enroll.error"));
            }
        }
    }

    @NotifyChange({"newDevice", "editingId"})
    public void prepareForUpdate(OTPDevice dev) {
        //This will make the modal window to become visible
        editingId = dev.getId();
        newDevice = new OTPDevice();
        newDevice.setNickName(dev.getNickName());
    }

    @NotifyChange({"editingId", "newDevice"})
    public void cancelUpdate(Event event) {
        newDevice.setNickName(null);
        editingId = 0;
        if (event != null && event.getName().equals(Events.ON_CLOSE)) {
            event.stopPropagation();
        }
    }

    @NotifyChange({"devices", "editingId", "newDevice"})
    public void update() {

        String nick = newDevice.getNickName();
        if (Utils.isNotEmpty(nick)) {
            //Find the index of the current device in the device list
            int i = Utils.firstTrue(devices, dev -> dev.getId() == editingId);
            OTPDevice dev = devices.get(i);
            //Updates its nickname
            dev.setNickName(nick);
            cancelUpdate(null);     //This doesn't undo anything we already did (just controls UI aspects)

            try {
                otpService.updateDevicesAdd(user.getId(), devices, null);
                UIUtils.showMessageUI(true);
            } catch (Exception e) {
                UIUtils.showMessageUI(false);
                logger.error(e.getMessage(), e);
            }
        }

    }

    private byte[] getSecretKeyFrom(String skey) {

        try {
            skey = skey.replaceAll("\\s", "");
            byte[] sk = new byte[skey.length() / 2];

            for (int i = 0; i < sk.length; i++) {
                sk[i] = (byte) (Integer.valueOf(skey.substring(i * 2, (i + 1) * 2), 16).intValue());
            }
            return sk;

        } catch (Exception e) {
            logger.warn("Computing key based on key typed for hard token failed");
            logger.error(e.getMessage(), e);
            UIUtils.showMessageUI(Clients.NOTIFICATION_TYPE_ERROR, Labels.getLabel("usr.otp_entered_key_wrong"));
        }
        return null;

    }

    private boolean enroll() {

        boolean success = false;
        try {
            logger.trace("Updating/adding device {} for user {}", newDevice.getNickName(), user.getId());
            newDevice.setAddedOn(new Date().getTime());
            newDevice.setSoft(tokenType.equals("SOFT"));
            newDevice.setTimeBased(newDevice.getSoft() || OTPType.TOTP.equals(hardTokenType));
            otpService.updateDevicesAdd(user.getId(), devices, newDevice);
            success = true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    private void focus(HtmlBasedComponent component) {
        if (component != null){
            component.focus();
        }
    }

    public void delete(OTPDevice device) {

        String resetMessages = resetPreferenceMessage(OTPExtension.ACR, devices.size());
        boolean reset = resetMessages != null;
        Pair<String, String> delMessages = getDeleteMessages(device.getNickName(), resetMessages);

        Messagebox.show(delMessages.getY(), delMessages.getX(), Messagebox.YES | Messagebox.NO,
                reset ? Messagebox.EXCLAMATION : Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        try {
                            devices.remove(device);
                            boolean success = otpService.updateDevicesAdd(user.getId(), devices, null);

                            if (success) {
                                if (reset) {
                                    userService.turn2faOff(user);
                                }
                                //trigger refresh (this method is asynchronous...)
                                BindUtils.postNotifyChange(OTPViewModel.this, "devices");
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

}
