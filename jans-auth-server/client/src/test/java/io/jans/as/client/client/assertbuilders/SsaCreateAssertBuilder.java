package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.ssa.create.SsaCreateRequest;
import io.jans.as.client.ssa.create.SsaCreateResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import org.apache.http.HttpStatus;

import static io.jans.as.model.ssa.SsaRequestParam.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SsaCreateAssertBuilder extends BaseAssertBuilder {

    private final SsaCreateRequest request;
    private final SsaCreateResponse response;
    private int status = HttpStatus.SC_CREATED;

    public SsaCreateAssertBuilder(SsaCreateRequest request, SsaCreateResponse response) {
        this.request = request;
        this.response = response;
    }

    public SsaCreateAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    @Override
    public void check() {
        assertNotNull(response, "SsaCreateResponse is null");
        assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getStatus());
        if (status == HttpStatus.SC_CREATED) {
            assertNotNull(response.getEntity(), "The entity is null");
            assertNotNull(response.getSsa(), "The ssa token is null");

            Jwt jwt = Jwt.parseSilently(response.getSsa());
            assertNotNull(jwt, "The jwt is null");
            JwtClaims jwtClaims = jwt.getClaims();
            assertNotNull(jwtClaims.getClaim(ORG_ID.getName()), "The org_id in jwt is null");
            assertEquals(jwtClaims.getClaimAsLong(ORG_ID.getName()), request.getOrgId());
            assertNotNull(jwtClaims.getClaim(SOFTWARE_ID.getName()), "The software_id in jwt is null");
            assertEquals(jwtClaims.getClaimAsString(SOFTWARE_ID.getName()), request.getSoftwareId());
            assertNotNull(jwtClaims.getClaim(SOFTWARE_ROLES.getName()), "The software_roles in jwt is null");
            assertEquals(jwtClaims.getClaimAsStringList(SOFTWARE_ROLES.getName()), request.getSoftwareRoles());
            assertNotNull(jwtClaims.getClaim(GRANT_TYPES.getName()), "The grant_types in jwt is null");
            assertEquals(jwtClaims.getClaimAsStringList(GRANT_TYPES.getName()), request.getGrantTypes());

            assertNotNull(jwtClaims.getClaim(JTI.getName()), "The jti in jwt is null");
            assertNotNull(jwtClaims.getClaim(ISS.getName()), "The iss in jwt is null");
            assertNotNull(jwtClaims.getClaim(IAT.getName()), "The iat in jwt is null");
            assertNotNull(jwtClaims.getClaim(EXP.getName()), "The exp in jwt is null");
            if (request.getExpiration() != null) {
                assertEquals(jwtClaims.getClaimAsLong(EXP.getName()), request.getExpiration());
            }
        } else {
            assertEquals(response.getStatus(), status, "Unexpected HTTP status response: " + response.getEntity());
            assertNotNull(response.getEntity(), "The entity is null");
            assertNotNull(response.getErrorDescription());
        }
    }
}
