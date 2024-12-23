package io.jans.casa.plugins.authnmethod.rs;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.service.cache.CacheProvider;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.jans.casa.core.PersistenceService;
import io.jans.casa.core.model.Person;
import io.jans.casa.core.pojo.FidoDevice;
import io.jans.casa.core.pojo.SecurityKey;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.rs.status.u2f.FinishCode;
import io.jans.casa.plugins.authnmethod.rs.status.u2f.RegisterMessageCode;
import io.jans.casa.plugins.authnmethod.rs.status.u2f.RegistrationCode;
import io.jans.casa.plugins.authnmethod.service.Fido2Service;
import io.jans.casa.rest.ProtectedApi;
import org.slf4j.Logger;

@ApplicationScoped
@ProtectedApi(scopes = "https://jans.io/casa.enroll")
@Path("/enrollment/fido2")
@Produces(MediaType.APPLICATION_JSON)
public class PasskeysEnrollingWS {

    private static final String USERS_PENDING_REG_PREFIX = "casa_upreg_";    //Users with pending registrations
    private static final String RECENT_DEVICES_PREFIX = "casa_recdev_";     //Recently enrolled devices
    private static final int EXPIRATION = (int) TimeUnit.MINUTES.toSeconds(2);

    @Inject
    private Logger logger;

    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private Fido2Service fido2Service;

    @Inject
    private PersistenceService persistenceService;

    private ObjectMapper mapper;

    @GET
    @Path("attestation")
    public Response getAttestationMessage(@QueryParam("userid") String userId) {

        String request = null;
        RegisterMessageCode result;
        logger.trace("getAttestationMessage WS operation called");

        if (Utils.isEmpty(userId)) {
            result = RegisterMessageCode.NO_USER_ID;
        } else {
            Person person = persistenceService.get(Person.class, persistenceService.getPersonDn(userId));
            if (person == null) {
                result = RegisterMessageCode.UNKNOWN_USER_ID;
            } else {
                try {
                    String userName = person.getUid();
                    request = fido2Service.doRegister(userName,
                                Optional.ofNullable(person.getGivenName()).orElse(userName));
                    result = RegisterMessageCode.SUCCESS;
                    cacheProvider.put(EXPIRATION, USERS_PENDING_REG_PREFIX + userId, "");
                } catch (Exception e) {
                    result = RegisterMessageCode.FAILED;
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return result.getResponse(request);

    }

    @POST
    @Path("registration/{userid}")
    public Response sendRegistrationResult(String body, @PathParam("userid") String userId) {

        RegistrationCode result;
        FidoDevice newDevice = null;
        logger.trace("sendRegistrationResult WS operation called");
        String key = USERS_PENDING_REG_PREFIX + userId;

        if (cacheProvider.hasKey(key)) {
            cacheProvider.remove(key);

            Person person = persistenceService.get(Person.class, persistenceService.getPersonDn(userId));
            if (person == null) {
                result = RegistrationCode.UNKNOWN_USER_ID;
            } else {
                try {
                    if (fido2Service.verifyRegistration(body)) {
                        newDevice = fido2Service.getLatestPasskey(userId, System.currentTimeMillis());

                        if (newDevice == null){
                            logger.info("Entry of recently registered fido 2 key could not be found for user {}", userId);
                            result = RegistrationCode.FAILED;
                        } else {
                            cacheProvider.put(EXPIRATION, RECENT_DEVICES_PREFIX + newDevice.getId(), userId);
                            result = RegistrationCode.SUCCESS;
                        }
                    } else {
                        logger.error("Verification has failed. See fido2 logs");
                        result = RegistrationCode.FAILED;
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    result = RegistrationCode.FAILED;
                }
            }

        } else {
            result = RegistrationCode.NO_MATCH_OR_EXPIRED;
        }
        return result.getResponse(newDevice);

    }

    @POST
    @Path("creds/{userid}")
    public Response nameEnrollment(NamedCredential credential,
                                   @PathParam("userid") String userId) {

        logger.trace("nameEnrollment WS operation called");
        String nickName = Optional.ofNullable(credential).map(NamedCredential::getNickName).orElse(null);
        String deviceId = Optional.ofNullable(credential).map(NamedCredential::getKey).orElse(null);

        FinishCode result;

        if (Stream.of(nickName, deviceId).anyMatch(Utils::isEmpty)) {
            result = FinishCode.MISSING_PARAMS;
        } else if (!cacheProvider.hasKey(RECENT_DEVICES_PREFIX + deviceId)) {
            result = FinishCode.NO_MATCH_OR_EXPIRED;
        } else {
            SecurityKey key = new SecurityKey();
            key.setId(deviceId);
            key.setNickName(nickName);

            if (fido2Service.updateDevice(key)) {
                result = FinishCode.SUCCESS;
                cacheProvider.remove(RECENT_DEVICES_PREFIX + deviceId);
            } else {
                result = FinishCode.FAILED;
            }
        }
        return result.getResponse();

    }

    @PostConstruct
    private void init() {
        logger.trace("Service inited");
        mapper = new ObjectMapper();
    }

}
