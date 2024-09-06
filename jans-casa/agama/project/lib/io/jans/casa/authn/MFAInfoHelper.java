package io.jans.casa.authn;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;

import io.jans.casa.CasaWSBase;

import java.util.*;
import java.net.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MFAInfoHelper extends CasaWSBase {
    
    public List<String> methodsEnrolled(String inum) {

        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.GET, 
                new URL(getApiBase() + "/2fa/user-info/" + URLEncoder.encode(inum, UTF_8)));
                
        Map<String, Object> response = sendRequest(request, true, true).getContentAsJSONObject();
        
        return (List<String>) response.get("enrolled_methods");

    }
    
}
