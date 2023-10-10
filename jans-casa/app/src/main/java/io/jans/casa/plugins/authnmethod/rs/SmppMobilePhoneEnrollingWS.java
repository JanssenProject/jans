package io.jans.casa.plugins.authnmethod.rs;

import io.jans.casa.plugins.authnmethod.OTPSmppExtension;
import io.jans.casa.plugins.authnmethod.service.SmppMobilePhoneService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

/**
 * @author Stefan Andersson
 */
@ApplicationScoped
@Path("/enrollment/" + OTPSmppExtension.ACR)
public class SmppMobilePhoneEnrollingWS extends MobilePhoneEnrollingWS {

    @Inject
    private SmppMobilePhoneService smppService;

    @PostConstruct
    private void init() {
        mobilePhoneService = smppService;
    }

}
