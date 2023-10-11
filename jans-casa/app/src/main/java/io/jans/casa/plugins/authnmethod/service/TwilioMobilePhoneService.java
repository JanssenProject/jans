package io.jans.casa.plugins.authnmethod.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.OTPTwilioExtension;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.stream.Stream;

/**
 * An app. scoped bean to serve the purpose of sending SMS using the Twilio service
 * @author jgomer
 */
@ApplicationScoped
public class TwilioMobilePhoneService extends MobilePhoneService {

    @Inject
    private Logger logger;

    private String fromNumber;
    private boolean initialized;

    @PostConstruct
    private void inited() {
        reloadConfiguration();
    }

    @Override
    public void reloadConfiguration() {

        initialized = false;
        props = persistenceService.getCustScriptConfigProperties(OTPTwilioExtension.ACR);

        if (props == null) {
            logger.warn("Config. properties for custom script '{}' could not be read. Features related to {} will not be accessible",
                    OTPTwilioExtension.ACR, OTPTwilioExtension.ACR.toUpperCase());
        } else {
            String sid = props.get("twilio_sid");
            String token = props.get("twilio_token");
            fromNumber = props.get("from_number");

            if (Stream.of(sid, token, fromNumber).anyMatch(Utils::isEmpty)) {
                logger.warn("Error parsing SMS settings. Please check LDAP entry of SMS custom script");
            } else {
                try {
                    Twilio.init(sid, token);
                    initialized = true;
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }

    @Override
    public SMSDeliveryStatus sendSMS(String number, String body) {

        SMSDeliveryStatus status;
        if (initialized) {

            try {
                Message message = Message.creator(new PhoneNumber("+" + number), new PhoneNumber(fromNumber), body).create();
                Message.Status statusMsg = message.getStatus();

                logger.info("Message delivery status was {}", statusMsg.toString());
                switch (statusMsg) {
                    case FAILED:
                        status = SMSDeliveryStatus.DELIVERY_FAILED;
                        break;
                    case UNDELIVERED:
                        status = SMSDeliveryStatus.UNDELIVERED;
                        break;
                    default:
                        status = SMSDeliveryStatus.SUCCESS;
                        logger.info("Message \"{}\" sent to #{}", body, number);
                }
            } catch (Exception e) {
                status = SMSDeliveryStatus.PROVIDER_ERROR;
                logger.error("No message was sent, error was: {}", e.getMessage());
            }
        } else {
            status = SMSDeliveryStatus.APP_SETUP_ERROR;
            logger.info("No message was sent, messageFactory was not initialized properly");
        }
        return status;

    }

}
