/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.uma;

import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaTokenResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

/**
 * @author yuriyz on 06/21/2017.
 */
public interface UmaTokenService {

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    UmaTokenResponse requestRpt(
            @HeaderParam("Authorization") String authorization,
            @FormParam("grant_type") String grantType,
            @FormParam("ticket") String ticket,
            @FormParam("claim_token") String claimToken,
            @FormParam("claim_token_format") String claimTokenFormat,
            @FormParam("pct") String pctCode,
            @FormParam("rpt") String rptCode,
            @FormParam("scope") String scope);

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    UmaTokenResponse requestJwtAuthorizationRpt(
            @FormParam("client_assertion_type") String clientAssertionType,
            @FormParam("client_assertion") String clientAssertion,
            @FormParam("grant_type") String grantType,
            @FormParam("ticket") String ticket,
            @FormParam("claim_token") String claimToken,
            @FormParam("claim_token_format") String claimTokenFormat,
            @FormParam("pct") String pctCode,
            @FormParam("rpt") String rptCode,
            @FormParam("scope") String scope);
}
