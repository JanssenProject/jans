package io.jans.casa.plugins.authnmethod.rs;

import io.jans.casa.plugins.authnmethod.service.TwilioMobilePhoneService;
import io.jans.casa.rest.ProtectedApi;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

@ApplicationScoped
@ProtectedApi(scopes = "https://jans.io/casa.enroll")
@Path("/enrollment/twilio_sms")
public class TwilioMobilePhoneEnrollingWS extends MobilePhoneEnrollingWS {

    @Inject
    private TwilioMobilePhoneService twilioService;

    @PostConstruct
    private void init() {
        mobilePhoneService = twilioService;
    }

}
