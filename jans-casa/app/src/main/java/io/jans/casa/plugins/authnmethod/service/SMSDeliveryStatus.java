package io.jans.casa.plugins.authnmethod.service;

/**
 * @author jgomer
 */
public enum SMSDeliveryStatus {
    /**
     * The underlying Twilio API call returned a status other than {@link #UNDELIVERED} or {@link #DELIVERY_FAILED}.
     * See Twilio <a href="https://www.twilio.com/docs/api/messaging/message#message-status-values">status values</a>.
     * <p>Recall that a success status does not guarantee the recipient actually receiving a message.</p>
     */
    SUCCESS,
    /**
     * The API call received no phone number to send the message to
     */
    NO_NUMBER,
    /**
     * The underlying Twilio API call returned a status of "undelivered".
     * See Twilio <a href="https://www.twilio.com/docs/api/messaging/message#message-status-values">status values</a>.
     */
    UNDELIVERED,
    /**
     * The underlying Twilio API call returned a status of "failed".
     * See Twilio <a href="https://www.twilio.com/docs/api/messaging/message#message-status-values">status values</a>.
     */
    DELIVERY_FAILED,
    /**
     * The message was not delivered due to a misconfiguration in the connection with the underlying Twilio service
     */
    APP_SETUP_ERROR,
    /**
     * The underlying Twilio API call threw an exception. This may happen in a variety of situations, for instance, providing
     * a non-valid phone number (e.g 555-1234-AB)
     */
    PROVIDER_ERROR;

}
