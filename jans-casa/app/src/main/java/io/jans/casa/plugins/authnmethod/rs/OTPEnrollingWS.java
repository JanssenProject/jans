package io.jans.casa.plugins.authnmethod.rs;

import io.jans.service.cache.CacheProvider;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.jans.casa.core.pojo.OTPDevice;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.rs.status.otp.ComputeRequestCode;
import io.jans.casa.plugins.authnmethod.rs.status.otp.FinishCode;
import io.jans.casa.plugins.authnmethod.rs.status.otp.ValidateCode;
import io.jans.casa.plugins.authnmethod.service.OTPService;
import io.jans.casa.plugins.authnmethod.service.otp.IOTPAlgorithm;
import io.jans.casa.rest.ProtectedApi;
import org.slf4j.Logger;

import static com.lochbridge.oath.otp.keyprovisioning.OTPKey.OTPType;

@ApplicationScoped
@ProtectedApi(scopes = "https://jans.io/casa.enroll")
@Path("/enrollment/otp")
@Produces(MediaType.APPLICATION_JSON)
public class OTPEnrollingWS {

    private static final String KEY_EXTERNALUID_MAPPING_PREFIX = "casa_kemp_";
    private static final int EXPIRATION = (int) TimeUnit.MINUTES.toSeconds(2);

    @Inject
    private Logger logger;

    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private OTPService otpService;
    
    @GET
    @Path("qr-request")
    public Response computeRequest(@QueryParam("displayName") String name,
                                   @QueryParam("mode") String mode) {

        logger.trace("computeRequest WS operation called");

        if (Utils.isEmpty(name)) {
            return ComputeRequestCode.NO_DISPLAY_NAME.getResponse();
        } else {
            try {
                IOTPAlgorithm as = otpService.getAlgorithmService(OTPType.valueOf(mode.toUpperCase()));
                byte[] secretKey = as.generateSecretKey();
                String request = as.generateSecretKeyUri(secretKey, name);

                return ComputeRequestCode.SUCCESS.getResponse(
                            Base64.getUrlEncoder().encodeToString(secretKey), request);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return ComputeRequestCode.INVALID_MODE.getResponse();
            }
        }

    }

    @POST
    @Path("validate-code")
    public Response validateCode(@FormParam("code") String code,
                                 @FormParam("key") String key,
                                 @FormParam("mode") String mode) {

        ValidateCode result;
        logger.trace("validateCode WS operation called");

        if (Stream.of(code, key, mode).anyMatch(Utils::isEmpty)) {
            result = ValidateCode.MISSING_PARAMS;
        } else {
            try {
                IOTPAlgorithm as = otpService.getAlgorithmService(OTPType.valueOf(mode.toUpperCase()));
                try {
                    String uid = as.getExternalUid(key, code);
                    if (uid == null) {
                        result = ValidateCode.NO_MATCH;
                    } else {
                        cacheProvider.put(EXPIRATION, KEY_EXTERNALUID_MAPPING_PREFIX + key, uid);
                        result = ValidateCode.MATCH;
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);                  
                    result = ValidateCode.FAILURE;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                result = ValidateCode.INVALID_MODE;
            }
        }
        return result.getResponse();

    }

    @GET
    @Path("creds/{userid}/{id}")
    public Response getEnrollments() {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @POST
    @Path("creds/{userid}")
    public Response finishEnrollment(NamedCredential credential,
                                     @PathParam("userid") String userId) {

        logger.trace("finishEnrollment WS operation called");
        FinishCode result;
        OTPDevice device = null;
            
        try {
            String nickName = Optional.ofNullable(credential).map(NamedCredential::getNickName).orElse(null);
            String key = Optional.ofNullable(credential).map(NamedCredential::getKey).orElse(null);
    
            if (Stream.of(nickName, key).anyMatch(Utils::isEmpty)) {
                result = FinishCode.MISSING_PARAMS;
            } else {
            
                key  = KEY_EXTERNALUID_MAPPING_PREFIX  + key;
                long now = System.currentTimeMillis();
                Object value = cacheProvider.get(key);
    
                if (value == null) {
                    result = FinishCode.NO_MATCH_OR_EXPIRED;
                } else {
                    device = new OTPDevice();
                    device.setUid(value.toString());
                    device.setAddedOn(now);
                    device.setNickName(nickName);
                    device.setSoft(true);
    
                    if (otpService.addDevice(userId, device)) {
                        result = FinishCode.SUCCESS;
                        cacheProvider.remove(key);
                    } else {
                        result = FinishCode.FAILED;
                        device = null;
                    }
                }            
            }
        
        } catch (Exception e) {
            result = FinishCode.FAILED;
            logger.error(e.getMessage(), e);
        }
        return result.getResponse(device);

    }

}
