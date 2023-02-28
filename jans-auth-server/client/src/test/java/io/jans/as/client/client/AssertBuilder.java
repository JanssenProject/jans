/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client;

import io.jans.as.client.*;
import io.jans.as.client.client.assertbuilders.*;
import io.jans.as.client.par.ParResponse;
import io.jans.as.client.ssa.create.SsaCreateRequest;
import io.jans.as.client.ssa.create.SsaCreateResponse;
import io.jans.as.client.ssa.get.SsaGetResponse;
import io.jans.as.client.ssa.jwtssa.SsaGetJwtResponse;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwt.Jwt;

public class AssertBuilder {

    public static ParResponseAssertBuilder parResponse(ParResponse response) {
        return new ParResponseAssertBuilder(response);
    }

    public static RegisterResponseAssertBuilder registerResponse(RegisterResponse response) {
        return new RegisterResponseAssertBuilder(response);
    }

    public static TokenResponseAssertBuilder tokenResponse(TokenResponse response) {
        return new TokenResponseAssertBuilder(response);
    }

    public static UserInfoResponseAssertBuilder userInfoResponse(UserInfoResponse response) {
        return new UserInfoResponseAssertBuilder(response);
    }

    public static ClientInfoResponseAssertBuilder clientInfoResponse(ClientInfoResponse response) {
        return new ClientInfoResponseAssertBuilder(response);
    }

    public static BackchannelAuthenticationResponseAssertBuilder backchannelAuthenticationResponse(BackchannelAuthenticationResponse response) {
        return new BackchannelAuthenticationResponseAssertBuilder(response);
    }

    public static AuthorizationResponseAssertBuilder authorizationResponse(AuthorizationResponse response) {
        return new AuthorizationResponseAssertBuilder(response);
    }

    public static JwtAssertBuilder jwt(Jwt jwt) {
        return new JwtAssertBuilder(jwt);
    }

    public static JwtAssertBuilder jwtParse(String jwtToken) throws InvalidJwtException {
        return new JwtAssertBuilder(Jwt.parse(jwtToken));
    }

    public static JweAssertBuilder jwe(Jwe jwe) {
        return new JweAssertBuilder(jwe);
    }

    public static SsaCreateAssertBuilder ssaCreate(SsaCreateRequest request, SsaCreateResponse response) {
        return new SsaCreateAssertBuilder(request, response);
    }

    public static SsaGetAssertBuilder ssaGet(SsaGetResponse response) {
        return new SsaGetAssertBuilder(response);
    }

    public static SsaGetJwtAssertBuilder ssaGetJwt(SsaGetJwtResponse response) {
        return new SsaGetJwtAssertBuilder(response);
    }
}
