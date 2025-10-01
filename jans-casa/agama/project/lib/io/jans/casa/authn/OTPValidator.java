package io.jans.casa.authn;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import io.jans.casa.CasaWSBase;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.StringJoiner;

public class OTPValidator extends CasaWSBase {

    public OTPValidator() throws IOException {
        super();
        setScope(SCOPE_PREFIX + "casa.2fa");
    }

    public boolean check(String id, String code) throws IOException {

        try {    
            HTTPRequest request = new HTTPRequest(HTTPRequest.Method.POST, 
                new URL(getApiBase() + "/validation/otp/verify-code"));
    
            StringJoiner joiner = new StringJoiner("&");
            Map.of("code", code, "userid", id).forEach((k, v) -> joiner.add(k + "=" + v));
            request.setQuery(joiner.toString());
            
            logger.debug("Verifying code supplied is valid against user's OTP credentials");            
            HTTPResponse response = sendRequest(request, false, true);
            String content = response.getContent().strip();     //BEWARE: stripping is important!
        
            if (response.getStatusCode() != 200) throw new Exception(content);
            return Boolean.valueOf(content);
        } catch (Exception e) {
            throw new IOException("Failed to determine if the passcode is valid", e);
        }
            
    }
    
}