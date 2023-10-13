package io.jans.casa.plugins.authnmethod.rs;

import io.jans.casa.plugins.authnmethod.OTPTwilioExtension;
import io.jans.casa.plugins.authnmethod.service.TwilioMobilePhoneService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

/**
 * @author Stefan Andersson
 */
@ApplicationScoped
@Path("/enrollment/" + OTPTwilioExtension.ACR)
public class TwilioMobilePhoneEnrollingWS extends MobilePhoneEnrollingWS {

    @Inject
    private TwilioMobilePhoneService twilioService;

    @PostConstruct
    private void init() {
        mobilePhoneService = twilioService;
    }

}
