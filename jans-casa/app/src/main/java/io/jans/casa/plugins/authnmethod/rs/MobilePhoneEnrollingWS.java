package io.jans.casa.plugins.authnmethod.rs;

import io.jans.service.cache.CacheProvider;

import java.security.SecureRandom;
import java.util.regex.Pattern;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.jans.casa.core.PersistenceService;
import io.jans.casa.core.model.Person;
import io.jans.casa.core.pojo.VerifiedMobile;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.rs.status.otp.FinishCode;
import io.jans.casa.plugins.authnmethod.rs.status.sms.SendCode;
import io.jans.casa.plugins.authnmethod.rs.status.sms.ValidateCode;
import io.jans.casa.plugins.authnmethod.service.MobilePhoneService;
import io.jans.casa.plugins.authnmethod.service.SMSDeliveryStatus;
import io.jans.casa.rest.ProtectedApi;
import org.slf4j.Logger;
import org.zkoss.util.resource.Labels;

import static io.jans.casa.plugins.authnmethod.service.SMSDeliveryStatus.SUCCESS;

/**
 * @author jgomer
 */
@ProtectedApi( scopes = "https://jans.io/casa.enroll" )
public class MobilePhoneEnrollingWS {

    private static final String SEPARATOR = ",";
    private static final String RECENT_CODES_PREFIX = "casa_reccode_";
    private static final int EXPIRATION = (int) TimeUnit.MINUTES.toSeconds(2);
    private static final String EXPIRY_SEPARATOR = "|";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    private Logger logger;

    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private PersistenceService persistenceService;

    MobilePhoneService mobilePhoneService;

    @GET
    @Path("send-code")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendCode(@QueryParam("number") String number,
                             @QueryParam("userid") String userId) {

        SendCode result;
        String message = null;
        logger.trace("sendCode WS operation called");

        if (Stream.of(number, userId).anyMatch(Utils::isEmpty)) {
            result = SendCode.MISSING_PARAMS;
        } else if (persistenceService.get(Person.class, persistenceService.getPersonDn(userId)) == null) {
            result = SendCode.UNKNOWN_USER_ID;
        } else if (mobilePhoneService.isNumberRegistered(number)
                || mobilePhoneService.isNumberRegistered(number.replaceAll("[-\\+\\s]", ""))) {
            result = SendCode.NUMBER_ALREADY_ENROLLED;
        } else {
            try {
                String aCode = Integer.toString(100000 + RANDOM.nextInt(900000));
                //Compose SMS body
                String body = Labels.getLabel("usr.mobile_sms_body", new String[]{ aCode });
                //Numbers are stored without the prepending plus in database
                String aNumber = number.substring(number.charAt(0) == '+' ? 1 : 0);

                //Send message (service bean already knows all settings to perform this step)
                SMSDeliveryStatus deliveryStatus = mobilePhoneService.sendSMS(aNumber, body);
                if (deliveryStatus.equals(SUCCESS)) {
                    logger.trace("sendCode. code={}", aCode);
                    long expiresAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(EXPIRATION);
                    cacheProvider.put(EXPIRATION, RECENT_CODES_PREFIX + userId + SEPARATOR + aCode,
                            aNumber + EXPIRY_SEPARATOR + expiresAt);
                    result = SendCode.SUCCESS;
                } else {
                    result = SendCode.FAILURE;
                    message = deliveryStatus.toString();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                result = SendCode.FAILURE;
            }
        }
        return result.getResponse(message);

    }

    @GET
    @Path("validate-code")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCode(@QueryParam("code") String code,
                                 @QueryParam("userid") String userId) {

        ValidateCode result;
        logger.trace("validateCode WS operation called");

        if (Stream.of(code, userId).anyMatch(Utils::isEmpty)) {
            result = ValidateCode.MISSING_PARAMS;
        } else {
            Object value = cacheProvider.get(RECENT_CODES_PREFIX + userId + SEPARATOR + code);
            result = activeEntry(value, System.currentTimeMillis()) == null
                    ? ValidateCode.NO_MATCH_OR_EXPIRED : ValidateCode.MATCH;
        }
        return result.getResponse();

    }

    @GET
    @Path("creds/{userid}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEnrollments() {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @POST
    @Path("creds/{userid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response finishEnrollment(NamedCredential credential,
                                     @PathParam("userid") String userId) {

        logger.trace("finishEnrollment WS operation called");
        String nickName = Optional.ofNullable(credential).map(NamedCredential::getNickName).orElse(null);
        String code = Optional.ofNullable(credential).map(NamedCredential::getKey).orElse(null);

        FinishCode result;
        VerifiedMobile phone = null;

        if (Stream.of(nickName, code).anyMatch(Utils::isEmpty)) {
            //One or more params are missing
            result = FinishCode.MISSING_PARAMS;
        } else {
            String key = RECENT_CODES_PREFIX + userId + SEPARATOR + code;
            long now = System.currentTimeMillis();
            Object value = cacheProvider.get(key);
            if (value != null) {
                //Consume before enrolling so a concurrent request cannot reuse the code
                cacheProvider.remove(key);
            }

            String[] entry = activeEntry(value, now);
            if (entry == null) {
                //No match or past its stored expiry
                result = FinishCode.NO_MATCH_OR_EXPIRED;
            } else {
                phone = new VerifiedMobile();
                phone.setNickName(nickName);
                phone.setAddedOn(now);
                phone.setNumber(entry[0]);

                if (mobilePhoneService.addPhone(userId, phone)) {
                    result = FinishCode.SUCCESS;
                } else {
                    restoreCode(key, value, entry, System.currentTimeMillis());
                    result = FinishCode.FAILED;
                    phone = null;
                }
            }
        }
        return result.getResponse(phone);

    }

    //Returns {number, expiresAt} only while the stored expiry has not passed; cache TTL alone may lag it
    private String[] activeEntry(Object value, long now) {
        if (value == null) {
            return null;
        }
        String[] entry = value.toString().split(Pattern.quote(EXPIRY_SEPARATOR), 2);
        try {
            return entry.length == 2 && now < Long.parseLong(entry[1]) ? entry : null;
        } catch (NumberFormatException e) {
            logger.warn("Invalid expiry in cached enrollment code");
            return null;
        }
    }

    //A failed enrollment must not extend the code's original lifetime
    private void restoreCode(String key, Object value, String[] entry, long now) {
        long remainingSec = TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(entry[1]) - now);
        if (remainingSec > 0) {
            cacheProvider.put((int) remainingSec, key, value);
        }
    }

}
