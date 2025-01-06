/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.plugin.adminui.test;



import io.jans.ca.plugin.adminui.AdminUIBaseTest;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class OAuth2ResourceTest extends AdminUIBaseTest{

   
    /**
     *  Testing oauth2 GET configuration endpoint
     */	 
    @Parameters({"test.issuer", "adminUIConfigURL"})
   @Test
    public void getOAuth2Data(final String issuer, final String adminUIConfigURL) {
        log.info("getOAuth2Data() - accessToken:{}, issuer:{}, adminUIConfigURL:{}", accessToken, issuer, adminUIConfigURL);
        Builder request = getResteasyService().getClientBuilder(issuer+adminUIConfigURL);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        
        Response response = request.get();
        log.info("Response for getOAuth2Data() -  response:{}", response);
    }
	
	
   /**
     *  Testing api-protection-token GET endpoint
     */	 
    @Parameters({"test.issuer", "apiProtectionTokenURL", "ujwt"})
    @Test
    public void getApiProtectionTokenData(final String issuer, final String apiProtectionTokenURL, final String ujwt) {
        log.info("\n\n getApiProtectionTokenData() - accessToken:{}, issuer:{}, apiProtectionTokenURL:{}, ujwt:{}", accessToken, issuer, apiProtectionTokenURL, ujwt);
        Builder request = getResteasyService().getClientBuilder(issuer+apiProtectionTokenURL);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        request.property("ujwt", ujwt);
        Response response = request.get();
        log.info("\n\n Response for getApiProtectionTokenData() -  response:{}", response);
    }

  
}
