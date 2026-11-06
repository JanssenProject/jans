package io.jans.casa.plugins.authnmethod.rs;

import io.jans.casa.core.pojo.OTPDevice;
import io.jans.casa.plugins.authnmethod.service.OTPService;
import io.jans.casa.plugins.authnmethod.service.otp.*;
import io.jans.casa.rest.ProtectedApi;
import io.jans.service.cache.CacheProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.zkoss.util.Pair;

@ApplicationScoped
@ProtectedApi(scopes = "https://jans.io/casa.2fa")
@Path("/validation/otp")
@Produces(MediaType.APPLICATION_JSON)
public class OTPValidationWS {

    private static final String USED_TOTP_PREFIX = "casa_totp_used_";

    private static final Object[] TOTP_LOCKS = new Object[64];

    static {
        Arrays.setAll(TOTP_LOCKS, i -> new Object());
    }

    @Inject
    private Logger logger;

    @Inject
    private OTPService otpService;

    @Inject
    private TOTPAlgorithmService tAS;

    @Inject
    private HOTPAlgorithmService hAS;

    @Inject
    private CacheProvider cacheProvider;

    @POST
    @Path("verify-code")
    public Response verify(@FormParam("userid") String id, @FormParam("code") String code) {
        
        boolean match = false;
        String msg = "No match found";
        List<OTPDevice> enrollments = otpService.getDevices(id);
        
        try {            
            logger.info("Scanning user's TOTP devices for a match");
            Optional<OTPDevice> totp = enrollments.stream()
                    .filter(dev -> dev.isTimeBased() && tAS.validateKey(dev.getKey(), code)).findFirst();
            
            if (totp.isPresent()) {
                match = consumeTotp(id, totp.get().getKey(), code);
                logger.debug("Match found for {}, code reuse: {}", id, !match);
                return Response.ok(match).build();
            }
                        
            logger.info("Scanning user's HOTP devices for a match");
            for (OTPDevice dev : enrollments) {
                if (!match && !dev.isTimeBased()) {
                    
                    try {
                        int i = dev.currentMovingFactor();
                        if (i != -1) {                            
                            Pair<Boolean, Long> p = hAS.validateKey(dev.getKey(), code, i, null); 
                            match = p.getX() && otpService.updateMovingFactor(id, dev, p.getY().intValue());
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                    
                }
            }
            return Response.ok(match).build();
        } catch (Exception e) {
            msg = e.getMessage();
            logger.error(msg, e);
        }
        return Response.serverError().entity(msg).build();

    }

    private boolean consumeTotp(String userId, String key, String code) throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(String.join(":", userId, key, code).getBytes(StandardCharsets.UTF_8));
        String cacheKey = USED_TOTP_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        //CacheProvider has no atomic put-if-absent; this at least serializes check-and-put on this node
        synchronized (TOTP_LOCKS[Math.floorMod(cacheKey.hashCode(), TOTP_LOCKS.length)]) {
            if (cacheProvider.get(cacheKey) != null) return false;
            cacheProvider.put(2 * tAS.getTimeStep() + 1, cacheKey, true);
            return true;
        }

    }
    
}