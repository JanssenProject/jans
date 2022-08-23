package io.jans.util;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import io.jans.service.cdi.util.CdiUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NetworkUtils {

    public static HTTPResponse sendGet(String url, MultivaluedMap<String, String> headers,
            MultivaluedMap<String, String> parameters) throws MalformedURLException, IOException {
        
        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.GET, new URL(url));
        if (headers != null) {
            headers.forEach((k, v) -> request.setHeader(k, v.toArray(new String[0])));
        }
        if (parameters != null && !parameters.isEmpty()) {
            
            String query = "";
            for (String key : parameters.keySet()) {
                
                List<String> values = parameters.get(key);
                if (values != null && !values.isEmpty()) {
                    
                    String delim = "&" + URLEncoder.encode(key, UTF_8) + "=";
                    query += delim.substring(1) + values.stream().map(s -> URLEncoder.encode(s, UTF_8))
                                .collect(Collectors.joining(delim));
                }
            }
            request.setQuery(query);
        }        
        return request.send();
        
    }
    
    public static Map<String, Object> mapFromGetRequest(String url, MultivaluedMap<String, String> headers,
            MultivaluedMap<String, String> parameters, boolean ensureOKStatus)
            throws MalformedURLException, IOException, ParseException {
        
        HTTPResponse response = sendGet(url, headers, parameters);
        if (ensureOKStatus) {
            response.ensureStatusCode(Response.Status.OK.getStatusCode());
        }
        return response.getContentAsJSONObject();
        
    }
    
    public static Map<String, Object> mapFromGetRequestWithToken(String url, String bearerToken)
            throws MalformedURLException, IOException, ParseException {

        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>(
                Collections.singletonMap("Authorization", "Bearer " + bearerToken));
        return mapFromGetRequest(url, headers, null, true);

    }
    
    public static String urlBeforeContextPath() {
        HttpServletRequest req = CdiUtil.bean(HttpServletRequest.class);
        return req.getScheme() + "://" + req.getServerName();
    }

}
