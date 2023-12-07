package io.jans.casa.ui.vm.user;

import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.service.SmppMobilePhoneService;
import io.jans.casa.plugins.authnmethod.OTPSmppExtension;
import org.zkoss.bind.annotation.Init;

/**
 * This is the ViewModel of page smpp-phone-detail.zul. It controls the CRUD of verified phones
 */
public class SmppVerifiedPhoneViewModel extends VerifiedPhoneViewModel {

    public SmppVerifiedPhoneViewModel() {
        mpService = Utils.managedBean(SmppMobilePhoneService.class);
        ACR = OTPSmppExtension.ACR;
    }

    @Init(superclass = true)
    //This dummy method allows parents' init methods to be called
    //Beware: do not change the name of this method to childInit or init (see ZK MVVM book)
    public void subChildInit() { }

}
