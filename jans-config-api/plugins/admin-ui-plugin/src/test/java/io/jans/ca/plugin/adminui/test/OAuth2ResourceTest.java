/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.plugin.adminui.test;

import io.jans.as.common.model.registration.Client;

import static io.restassured.RestAssured.given;
import io.jans.ca.plugin.adminui.AdminUIBaseTest;
import io.jans.model.net.HttpServiceResponse;

import java.util.Map;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;

import org.apache.http.entity.ContentType;
import static org.testng.Assert.*;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class OAuth2ResourceTest extends AdminUIBaseTest{

   
    /**
     *  Testing oauth2 GET configuration endpoint
     */	 
    @Parameters({"issuer", "adminUIConfigURL"})
    @Test
    public void getOAuth2Data(final String issuer, final String adminUIConfigURL) {
        log.error("getOAuth2Data() - accessToken:{}, issuer:{}, adminUIConfigURL:{}", accessToken, issuer, adminUIConfigURL);
        Builder request = getResteasyService().getClientBuilder(issuer+adminUIConfigURL);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        
        Response response = request.get();
        log.error("Response for getOAuth2Data() -  response:{}", response);
    }
	
	
   /**
     *  Testing api-protection-token GET endpoint
     */	 
    @Parameters({"issuer", "apiProtectionTokenURL"})
    @Test
    public void getApiProtectionTokenData(final String issuer, final String apiProtectionTokenURL, final String ujwt) {
        log.error("getApiProtectionTokenData() - accessToken:{}, issuer:{}, apiProtectionTokenURL:{}, ujwt:{}", accessToken, issuer, apiProtectionTokenURL, ujwt);
        Builder request = getResteasyService().getClientBuilder(issuer+apiProtectionTokenURL);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        request.property("ujwt", ujwt);
        Response response = request.get();
        log.error("Response for getApiProtectionTokenData() -  response:{}", response);
    }

  
}
