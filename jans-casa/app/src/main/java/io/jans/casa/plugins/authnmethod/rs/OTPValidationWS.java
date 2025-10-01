package io.jans.casa.plugins.authnmethod.rs;

import io.jans.casa.core.pojo.OTPDevice;
import io.jans.casa.plugins.authnmethod.service.OTPService;
import io.jans.casa.plugins.authnmethod.service.otp.*;
import io.jans.casa.rest.ProtectedApi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

import org.slf4j.Logger;
import org.zkoss.util.Pair;

@ApplicationScoped
@ProtectedApi(scopes = "https://jans.io/casa.2fa")
@Path("/validation/otp")
@Produces(MediaType.APPLICATION_JSON)
public class OTPValidationWS {

    @Inject
    private Logger logger;

    @Inject
    private OTPService otpService;

    @Inject
    private TOTPAlgorithmService tAS;

    @Inject
    private HOTPAlgorithmService hAS;

    @POST
    @Path("verify-code")
    public Response verify(@FormParam("userid") String id, @FormParam("code") String code) {
        
        boolean match = false;
        String msg = "No match found";
        List<OTPDevice> enrollments = otpService.getDevices(id);
        
        try {            
            logger.info("Scanning user's TOTP devices for a match with {}", code);
            match = enrollments.stream().filter(dev -> dev.isTimeBased() && tAS.validateKey(dev.getKey(), code))
                    .findFirst().isPresent();                
            
            if (match) {
                logger.debug("Match found for {}", id);
                return Response.ok(match).build();
            }
                        
            logger.info("Scanning user's HOTP devices for a match with {}", code);
            for (OTPDevice dev : enrollments) {
                if (!match && !dev.isTimeBased()) {
                    
                    try {
                        int i = dev.currentMovingFactor();
                        if (i != -1) {                            
                            Pair<Boolean, Long> p = hAS.validateKey(dev.getKey(), code, i, null); 
                            match = p.getX();
                            
                            if (match) {
                                otpService.updateMovingFactor(id, dev, p.getY().intValue());
                            }
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
    
}