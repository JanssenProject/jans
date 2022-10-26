package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.SsaRequest;
import io.jans.as.client.SsaResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.ssa.SsaErrorResponseType;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SsaResponseAssertBuilder extends BaseAssertBuilder {

    private final SsaRequest request;
    private final SsaResponse response;
    private int status = 200;
    private SsaErrorResponseType errorResponseType;

    public SsaResponseAssertBuilder(SsaRequest request, SsaResponse response) {
        this.request = request;
        this.response = response;
    }

    public SsaResponseAssertBuilder ok() {
        this.status = 200;
        return this;
    }

    public SsaResponseAssertBuilder created() {
        this.status = 201;
        return this;
    }

    public SsaResponseAssertBuilder bad(SsaErrorResponseType responseType) {
        this.status = 400;
        this.errorResponseType = responseType;
        return this;
    }

    public SsaResponseAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public SsaResponseAssertBuilder errorResponseType(SsaErrorResponseType errorResponseType) {
        this.errorResponseType = errorResponseType;
        return this;
    }

    @Override
    public void check() {
        assertNotNull(response, "SsaResponse is null");
        if (status == 200 || status == 201) {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getStatus());
            assertNotNull(response.getEntity(), "The entity is null");
            assertNotNull(response.getSsa(), "The ssa token is null");

            Jwt jwt = Jwt.parseSilently(response.getSsa());
            assertNotNull(jwt, "The jwt is null");
            JwtClaims jwtClaims = jwt.getClaims();
            assertNotNull(jwtClaims.getClaim("org_id"), "The org_id in jwt is null");
            assertEquals(jwtClaims.getClaimAsLong("org_id"), request.getOrgId());
            assertNotNull(jwtClaims.getClaim("software_id"), "The software_id in jwt is null");
            assertEquals(jwtClaims.getClaimAsString("software_id"), request.getSoftwareId());
            assertNotNull(jwtClaims.getClaim("software_roles"), "The software_roles in jwt is null");
            assertEquals(jwtClaims.getClaimAsStringList("software_roles"), request.getSoftwareRoles());
            assertNotNull(jwtClaims.getClaim("grant_types"), "The grant_types in jwt is null");
            assertEquals(jwtClaims.getClaimAsStringList("grant_types"), request.getGrantTypes());

            assertNotNull(jwtClaims.getClaim("jti"), "The jti in jwt is null");
            assertNotNull(jwtClaims.getClaim("iss"), "The iss in jwt is null");
            assertNotNull(jwtClaims.getClaim("iat"), "The iat in jwt is null");
            assertNotNull(jwtClaims.getClaim("exp"), "The exp in jwt is null");
            if (request.getExpiration() != null) {
                assertEquals(jwtClaims.getClaimAsLong("exp"), request.getExpiration());
            }
        } else {
            assertEquals(response.getStatus(), status, "Unexpected HTTP status response: " + response.getEntity());
            assertNotNull(response.getEntity(), "The entity is null");
            if (errorResponseType != null) {
                assertEquals(response.getErrorType(), errorResponseType, "Unexpected error type, should be " + errorResponseType.getParameter());
            }
            assertNotNull(response.getErrorDescription());
        }
    }
}
