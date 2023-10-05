package io.jans.casa.plugins.authnmethod.rs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import io.jans.casa.core.pojo.OTPDevice;
import io.jans.casa.plugins.authnmethod.OTPExtension;
import io.jans.casa.plugins.authnmethod.service.OTPService;
import io.jans.casa.plugins.authnmethod.service.otp.HOTPAlgorithmService;
import io.jans.casa.plugins.authnmethod.service.otp.IOTPAlgorithm;
import io.jans.casa.rest.ProtectedApi;
import org.slf4j.Logger;

import static com.lochbridge.oath.otp.keyprovisioning.OTPKey.OTPType;

@ApplicationScoped
@ProtectedApi(scopes = "https://jans.io/casa.2fa")
@Path("/validation/" + OTPExtension.ACR)
@Produces(MediaType.APPLICATION_JSON)
public class OTPValidationWS {

    @Inject
    private Logger logger;

    @Inject
    private OTPService otpService;

    @Inject
    private HOTPAlgorithmService hAS;

    @POST
    @Path("verify-code")
    public Response verify(@FormParam("userid") String id, @FormParam("code") String code) {
        
        boolean match = false;
        String msg = "No match found";
        List<OTPDevice> enrollments = otpService.getDevices(id);
        
        try {
            String pref = OTPType.TOTP.getName().toLowerCase();
            IOTPAlgorithm as = otpService.getAlgorithmService(OTPType.TOTP);
            
            logger.info("Scanning user's TOTP devices for a match with {}", code);
            for (OTPDevice dev : enrollments) {
                if (!match && dev.isTimeBased()) {
                    match = as.getExternalUid(dev.getUid().replaceFirst(pref + ":", ""), code) != null;
                }
            }
            
            if (match) {
                logger.debug("Match found for {}", id);
                return Response.ok(match).build();
            }
             
            as = otpService.getAlgorithmService(OTPType.HOTP);
            pref = OTPType.HOTP.getName().toLowerCase();
            //TODO: validate hotp: 
            //make public the method HOTPAS#validateKey(byte[], String, int, Integer)
            //use hAS
            return Response.ok(match).build();
        } catch (Exception e) {
            msg = e.getMessage();
            logger.error(msg, e);
        }
        return Response.serverError().entity(msg).build();

    }
    
}