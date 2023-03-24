package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.ssa.jwtssa.SsaGetJwtResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import org.apache.http.HttpStatus;

import static io.jans.as.model.ssa.SsaRequestParam.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SsaGetJwtAssertBuilder extends BaseAssertBuilder {

    private final SsaGetJwtResponse response;
    private int status;

    public SsaGetJwtAssertBuilder(SsaGetJwtResponse response) {
        this.response = response;
    }

    public SsaGetJwtAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    @Override
    public void check() {
        assertNotNull(response, "SsaGetJwtResponse is null");
        assertEquals(response.getStatus(), status, "Unexpected HTTP status response: " + response.getStatus());
        if (status == HttpStatus.SC_OK) {
            assertNotNull(response.getEntity(), "The entity is null");
            assertNotNull(response.getSsa(), "The ssa token is null");

            Jwt jwt = Jwt.parseSilently(response.getSsa());
            assertNotNull(jwt, "The jwt is null");
            JwtClaims jwtClaims = jwt.getClaims();
            assertNotNull(jwtClaims.getClaim(ORG_ID.getName()), "The org_id in jwt is null");
            assertNotNull(jwtClaims.getClaim(SOFTWARE_ID.getName()), "The software_id in jwt is null");
            assertNotNull(jwtClaims.getClaim(SOFTWARE_ROLES.getName()), "The software_roles in jwt is null");
            assertNotNull(jwtClaims.getClaim(GRANT_TYPES.getName()), "The grant_types in jwt is null");

            assertNotNull(jwtClaims.getClaim(JTI.getName()), "The jti in jwt is null");
            assertNotNull(jwtClaims.getClaim(ISS.getName()), "The iss in jwt is null");
            assertNotNull(jwtClaims.getClaim(IAT.getName()), "The iat in jwt is null");
            assertNotNull(jwtClaims.getClaim(EXP.getName()), "The exp in jwt is null");
        }
    }
}
