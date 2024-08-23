package io.jans.casa.ui.vm.user;

import io.jans.casa.core.pojo.VerifiedMobile;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.service.SMSDeliveryStatus;
import io.jans.casa.plugins.authnmethod.service.MobilePhoneService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Messagebox;

/**
 * This is the ViewModel of page phone-detail-shared.zul. It controls the CRUD of verified phones
 */
public class VerifiedPhoneViewModel extends UserViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    String ACR;
    MobilePhoneService mpService;

    private boolean uiCodesMatch;
    private boolean uiSmsDelivered;

    private List<VerifiedMobile> phones;
    private VerifiedMobile newPhone;
    private String code;
    private String realCode;
    private String editingNumber;

    public String getEditingNumber() {
        return editingNumber;
    }

    public VerifiedMobile getNewPhone() {
        return newPhone;
    }

    public List<VerifiedMobile> getPhones() {
        return phones;
    }

    public String getCode() {
        return code;
    }

    public boolean isUiCodesMatch() {
        return uiCodesMatch;
    }

    public boolean isUiSmsDelivered() {
        return uiSmsDelivered;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setNewPhone(VerifiedMobile newPhone) {
        this.newPhone = newPhone;
    }

    @Init(superclass = true)
    public void childInit() throws Exception {
        newPhone = new VerifiedMobile(null);
        phones = mpService.getVerifiedPhones(user.getId());
    }

    @NotifyChange("uiSmsDelivered")
    public void sendCode(HtmlBasedComponent toFocus){

        //Note similar logic exists at REST service MobilePhoneEnrollingWS
        String numb = newPhone.getNumber();
        if (Utils.isNotEmpty(numb)) {   //Did user fill out the phone text box?
            //Check for uniquess throughout all phones in LDAP. Only new numbers are accepted
            try {
                if (mpService.isNumberRegistered(numb) || mpService.isNumberRegistered(numb.replaceAll("[-\\+\\s]", ""))) {
                    UIUtils.showMessageUI(Clients.NOTIFICATION_TYPE_WARNING, Labels.getLabel("usr.mobile_already_exists"));
                } else {
                    //Generate random in [100000, 999999]
                    realCode = Integer.toString(new Double(100000 + Math.random() * 899999).intValue());

                    String body = Labels.getLabel("usr.mobile_sms_body", new String[] { realCode });
                    logger.trace("sendCode. code={}", realCode);

                    //Send message (service bean already knows all settings to perform this step)
                    uiSmsDelivered = mpService.sendSMS(numb, body).equals(SMSDeliveryStatus.SUCCESS);
                    if (uiSmsDelivered) {
                        if (toFocus != null) {
                            toFocus.focus();
                        }
                    } else {
                        UIUtils.showMessageUI(false);
                    }
                }
            } catch (Exception e) {
                UIUtils.showMessageUI(false);
                logger.error(e.getMessage(), e);
            }
        }
    }

    @NotifyChange({ "uiCodesMatch", "uiSmsDelivered" })
    public void checkCode(HtmlBasedComponent toFocus) {
        uiCodesMatch = Utils.isNotEmpty(code) && Utils.isNotEmpty(realCode) && realCode.equals(code.trim());
        if (uiCodesMatch) {
            if (toFocus != null) {
                toFocus.focus();
            }
            uiSmsDelivered = false;
        } else {
            UIUtils.showMessageUI(Clients.NOTIFICATION_TYPE_WARNING, Labels.getLabel("usr.mobile_code_wrong"));
        }
    }

    @NotifyChange({"uiCodesMatch", "code", "phones", "newPhone"})
    public void add() {

        if (Utils.isNotEmpty(newPhone.getNickName())) {
            newPhone.setAddedOn(new Date().getTime());

            if (mpService.updateMobilePhonesAdd(user.getId(), phones, newPhone)) {
                UIUtils.showMessageUI(true, Labels.getLabel("usr.enroll.success"));
                Clients.response(new AuInvoke("resetPhoneValue"));
                userService.notifyEnrollment(user, ACR);
            } else {
                UIUtils.showMessageUI(false, Labels.getLabel("usr.enroll.error"));
            }
            cancel();
        }

    }

    @NotifyChange({"uiCodesMatch", "code", "newPhone", "uiSmsDelivered"})
    public void cancel() {
        uiCodesMatch = false;
        realCode = null;
        code = null;
        uiSmsDelivered = false;
        newPhone = new VerifiedMobile();
    }

    @NotifyChange({"newPhone", "editingNumber"})
    public void cancelUpdate(Event event){
        newPhone.setNickName(null);
        editingNumber = null;
        if (event != null && event.getName().equals(Events.ON_CLOSE)) {
            event.stopPropagation();
        }
    }

    @NotifyChange({"newPhone", "editingNumber"})
    public void prepareForUpdate(VerifiedMobile phone) {
        //This will make the modal window to become visible
        editingNumber = phone.getNumber();
        newPhone = new VerifiedMobile("");
        newPhone.setNickName(phone.getNickName());
    }

    @NotifyChange({"newPhone", "phones", "editingNumber"})
    public void update() {

        String nick = newPhone.getNickName();
        if (Utils.isNotEmpty(nick)) {
            int i = Utils.firstTrue(phones, p -> p.getNumber().equals(editingNumber));
            VerifiedMobile ph = phones.get(i);
            ph.setNickName(nick);
            cancelUpdate(null);

            try {
                mpService.updateMobilePhonesAdd(user.getId(), phones, null);
                UIUtils.showMessageUI(true);
            } catch (Exception e) {
                UIUtils.showMessageUI(false);
                logger.error(e.getMessage(), e);
            }
        }

    }

    public void delete(VerifiedMobile phone) {

        String resetMessages = resetPreferenceMessage(ACR, phones.size());
        boolean reset = resetMessages != null;
        Pair<String, String> delMessages = getDeleteMessages(phone.getNickName(), resetMessages);

        Messagebox.show(delMessages.getY(), delMessages.getX(), Messagebox.YES | Messagebox.NO,
                reset ? Messagebox.EXCLAMATION : Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        try {
                            phones.remove(phone);
                            boolean success = mpService.updateMobilePhonesAdd(user.getId(), phones, null);

                            if (success) {
                                if (reset) {
                                    userService.turn2faOff(user);
                                }
                                //trigger refresh (this method is asynchronous...)
                                BindUtils.postNotifyChange(VerifiedPhoneViewModel.this, "phones");
                            } else {
                                phones.add(phone);
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
