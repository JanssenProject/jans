package io.jans.as.client.client;

import io.jans.as.client.*;
import io.jans.as.client.client.assertbuilders.*;
import io.jans.as.client.par.ParResponse;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwt.Jwt;

public class AssertBuilder {

    public static ParResponseAssertBuilder parResponseBuilder(ParResponse response) {
        return new ParResponseAssertBuilder(response);
    }

    public static RegisterResponseAssertBuilder registerResponseBuilder(RegisterResponse response) {
        return new RegisterResponseAssertBuilder(response);
    }

    public static TokenResponseAssertBuilder tokenResponseBuilder(TokenResponse response) {
        return new TokenResponseAssertBuilder(response);
    }

    public static UserInfoResponseAssertBuilder userInfoResponseBuilder(UserInfoResponse response) {
        return new UserInfoResponseAssertBuilder(response);
    }

    public static BackchannelAuthenticationResponseAssertBuilder backchannelAuthenticationResponseBuilder(BackchannelAuthenticationResponse response) {
        return new BackchannelAuthenticationResponseAssertBuilder(response);
    }

    public static AuthorizationResponseAssertBuilder authorizationResponseBuilder(AuthorizationResponse response) {
        return new AuthorizationResponseAssertBuilder(response);
    }

    public static JwtAssertBuilder jwtBuilder(Jwt jwt) {
        return new JwtAssertBuilder(jwt);
    }

    public static JweAssertBuilder jweBuilder(Jwe jwe) {
        return new JweAssertBuilder(jwe);
    }

}
