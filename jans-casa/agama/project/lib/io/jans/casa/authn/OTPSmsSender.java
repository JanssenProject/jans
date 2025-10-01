package io.jans.casa.authn;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import io.jans.casa.CasaWSBase;
import io.jans.as.common.model.common.User;
import io.jans.as.server.service.*;
import io.jans.service.cdi.util.CdiUtil;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OTPSmsSender extends CasaWSBase {

    private static final String MOBILE_PHONE_ATTR = "mobile";
    
    public OTPSmsValidator() throws IOException {
        super();
        setScope(SCOPE_PREFIX + "casa.2fa");
    }

    public List<String> getPhoneNumbers(String inum) throws IOException {
        
        User loser = CdiUtil.bean(UserService.class).getUserByInum(inum, MOBILE_PHONE_ATTR);
        List<String> phones = Optional.ofNullable(loser)
                .map(usr -> usr.getAttributeValues(MOBILE_PHONE_ATTR)).orElse(Collections.emptyList());
        
        if (phones.isEmpty()) throw new IOException("Unable to determine the phone numbers associated to this account");
        
        return phones;
        
    }
    
    public String send(String phone) throws IOException {

        try {
            HTTPRequest request = new HTTPRequest(HTTPRequest.Method.POST, 
                new URL(getApiBase() + "/util/twilio_sms/send"));    
            request.setQuery("phoneNumber=" + URLEncoder.encode(phone, UTF_8));

            logger.info("Calling service for SMS delivery");            
            HTTPResponse response = sendRequest(request, false, true);

            String content = response.getContent();
            if (response.getStatusCode() != 200) throw new Exception(content);

            return content.strip();
        } catch (Exception e) {
            throw new IOException("Unable to deliver SMS", e);
        }
            
    }
    
    public List<String> mask(List<String> numbers) {
    
        int tail = 4;
        List<String> masked = new ArrayList<>();
        for (String number : numbers) {
            
            int ast = number.length() - tail; 
            if (ast > 0) {
                masked.add("*".repeat(ast) + number.substring(ast));                
            } else {
                masked.add(number);
            }
        }
        return masked;
        
    }

}
