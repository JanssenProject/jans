package io.jans.casa.plugins.authnmethod.rs;

import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.service.*;
import io.jans.casa.rest.ProtectedApi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.zkoss.util.resource.Labels;

import static io.jans.casa.plugins.authnmethod.service.SMSDeliveryStatus.SUCCESS;

@ApplicationScoped
@ProtectedApi(scopes = "https://jans.io/casa.2fa")
@Path("/util/twilio_sms")
@Produces(MediaType.TEXT_PLAIN)
public class SmsDeliveryWS {

    @Inject
    private Logger logger;

    @Inject
    private TwilioMobilePhoneService twilioService;
    
    @POST
    @Path("send")
    public Response send(@FormParam("phoneNumber") String number) {
        
        String result = null;
        Response.Status httpStatus;
        
        logger.trace("sendSms WS operation called");
        if (Utils.isEmpty(number) || !number.matches("\\d+")) {
            httpStatus = Response.Status.BAD_REQUEST;
        } else {
            
            try {
                if (number.startsWith("+")) {
                    number = number.substring(1);
                }
                String aCode = Integer.toString(new Double(100000 + Math.random() * 899999).intValue());
                //Compose SMS body
                String body = Labels.getLabel("usr.mobile_sms_body", new String[]{ aCode });
                SMSDeliveryStatus deliveryStatus = twilioService.sendSMS(number, body);
                
                if (deliveryStatus.equals(SUCCESS)) {
                    logger.trace("Code sent was {}", aCode);              
                    result = aCode;
                    httpStatus =  Response.Status.OK;
                } else {
                    result = deliveryStatus.toString();
                    httpStatus =  Response.Status.INTERNAL_SERVER_ERROR;
                }
            } catch (Exception e) {
                result = e.getMessage();
                httpStatus =  Response.Status.INTERNAL_SERVER_ERROR;
                logger.error(result, e);
            }
        }
        return Response.status(httpStatus).entity(result).build();
    
    }
    
}
