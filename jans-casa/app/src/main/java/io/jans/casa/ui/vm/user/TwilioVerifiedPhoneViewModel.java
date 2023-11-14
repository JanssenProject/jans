package io.jans.casa.ui.vm.user;

import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.service.TwilioMobilePhoneService;
import io.jans.casa.plugins.authnmethod.OTPTwilioExtension;
import org.zkoss.bind.annotation.Init;

/**
 * This is the ViewModel of page twilio-phone-detail.zul. It controls the CRUD of verified phones
 */
public class TwilioVerifiedPhoneViewModel extends VerifiedPhoneViewModel {

    public TwilioVerifiedPhoneViewModel() {
        mpService = Utils.managedBean(TwilioMobilePhoneService.class);
        ACR = OTPTwilioExtension.ACR;
    }

    @Init(superclass = true)
    //This dummy method allows parents' init methods to be called
    //Beware: do not change the name of this method to childInit or init (see ZK MVVM book)
    public void subChildInit() { }

}
