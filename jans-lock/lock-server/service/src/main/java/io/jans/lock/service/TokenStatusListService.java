/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.service;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import io.jans.as.client.JwkClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.StatusListClient;
import io.jans.as.client.StatusListRequest;
import io.jans.as.client.StatusListResponse;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.JwsSigner;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtHeaderName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Token service
 *
 * @author Yuriy Movchan Date: 01/05/2024
 */
@ApplicationScoped
public class TokenStatusListService {

    public static final String CONTENT_TYPE_STATUSLIST_JSON = "application/statuslist+json";
    public static final String CONTENT_TYPE_STATUSLIST_JWT = "application/statuslist+jwt";

    @Inject
    private Logger log;

    @Inject
    private OpenIdService openIdService;

    public StatusListResponse loadTokenStatusList() {
    	OpenIdConfigurationResponse openIdConfiguration = openIdService.getOpenIdConfiguration();
    	log.debug("Loaded OpenIdConfiguration: {}", openIdConfiguration);
    	
    	StatusListResponse statusListResponse = requestTokenStatusList(openIdConfiguration.getStatusListEndpoint());
    	if (statusListResponse == null) {
    		return null;
    	}

    	final Jwt jwt = statusListResponse.getJwt();
    	SignatureAlgorithm signatureAlg = jwt.getHeader().getSignatureAlgorithm();
    	AlgorithmFamily algFamily = signatureAlg.getFamily();
    	
    	JwsSigner jwtSigner;
    	if (AlgorithmFamily.RSA == algFamily) {
    		jwtSigner = new RSASigner(signatureAlg, JwkClient.getRSAPublicKey(openIdConfiguration.getJwksUri(), jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID)));
    	} else if (AlgorithmFamily.EC == algFamily) {
    		jwtSigner = new ECDSASigner(signatureAlg, JwkClient.getECDSAPublicKey(openIdConfiguration.getJwksUri(), jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID)));
    	} else {
    		log.error("Unsupported signature algorithm family: '{}'", algFamily);
    		return null;
    	}

        boolean isValid = jwtSigner.validate(jwt);
        if (!isValid) {
    		log.error("Token status list JWT signature is invalid");
    		return null;
        }
    	
        return statusListResponse;
    }

    private StatusListResponse requestTokenStatusList(String statusListEndpoint) {
        StatusListRequest statusListRequest = new StatusListRequest();
        StatusListClient statusListClient = new StatusListClient(statusListEndpoint);
        StatusListResponse statusListResponse = statusListClient.exec(statusListRequest);
        
        if (statusListResponse.getStatus() != HttpStatus.SC_OK) {
        	log.debug("Faield to load token status list");
        	return null;
        }
        
        return statusListResponse;
    }
}