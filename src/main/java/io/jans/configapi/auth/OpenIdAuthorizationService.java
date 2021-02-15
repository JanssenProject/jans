/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.configapi.auth.service.OpenIdService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;

@ApplicationScoped
@Named("openIdAuthorizationService")
public class OpenIdAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger log;

    @Inject
    OpenIdService openIdService;

    public void processAuthorization(String token, String issuer, String tokenType, ResourceInfo resourceInfo, String method, String path)
            throws Exception {
        log.info("oAuth  Authorization parameters , token:{}, issuer:{}, tokenType:{}, resourceInfo:{}, method: {}, path: {} ", token, issuer, tokenType, resourceInfo, method, path);
        if (StringUtils.isBlank(token)) {
            log.error("Token is blank !!!");
            throw new WebApplicationException("Token is blank.", Response.status(Response.Status.UNAUTHORIZED).build());
        }

        List<String> resourceScopes = getRequestedScopes(resourceInfo);
        log.info("oAuth  Authorization Resource details, resourceInfo: {}, resourceScopes: {} ", resourceInfo, resourceScopes);
        
        //Validate issuer
        if(StringUtils.isNotBlank(issuer) && !this.validIssuer(issuer)) {
        	throw new WebApplicationException("Header Issuer is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
        
        //Check the type of token simple, jwt, reference
        validateToken(token, issuer, tokenType,resourceScopes);
        
        IntrospectionResponse introspectionResponse = openIdService.getIntrospectionResponse(token, token.substring("Bearer".length()).trim(), issuer);
       
        if (introspectionResponse == null || !introspectionResponse.isActive()) {
            log.error("Token is Invalid.");
            throw new WebApplicationException("Token is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
        
        if (!validateScope(introspectionResponse.getScope(), resourceScopes)) {
            log.error("Insufficient scopes. Required scope: " + resourceScopes + ", token scopes: "
                    + introspectionResponse.getScope());
            throw new WebApplicationException("Insufficient scopes. Required scope",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

    }
    
    private void validateToken(String token, String issuer, String tokenType,List<String> resourceScopes) throws Exception{
    	log.info("oAuth  validateToken parameters , token:{}, issuer:{}, tokenType:{}, resourceScopes:{} ", token, issuer, tokenType,resourceScopes);
    	try {
    		Jwt jwt = parse(token);
    		log.info("JWT details : jwt.toString() - "+jwt.toString()+" , jwt.getClaims() "+jwt.getClaims()+" , jwt.getHeader() = "+jwt.getHeader());
    		
    		log.info("JWT details : jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER) - "+jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
    		log.info("JWT details : jwt.getClaims().getClaimAsString(scope) - "+jwt.getClaims().getClaimAsString("scope"));
    		
    		//Validate issuer
            if(!this.validIssuer(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER))) {
            	throw new WebApplicationException("Jwt Issuer is Invalid.",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }
    		
    	    if (!validateScope(jwt.getClaims().getClaimAsStringList("scope"), resourceScopes)) {
                log.error("Insufficient scopes. Required scope: " + resourceScopes + ", token scopes: "
                        + jwt.getClaims().getClaimAsStringList("scope"));
                throw new WebApplicationException("Insufficient scopes. Required scope",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }
    	}
    	catch(InvalidJwtException exp) {
    		log.error("Not a valid Jwt token = "+exp);
    	}
    	
    }

}