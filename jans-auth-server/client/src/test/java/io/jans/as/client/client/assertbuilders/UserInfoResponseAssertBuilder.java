package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.UserInfoResponse;
import io.jans.as.model.jwt.JwtClaimName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

public class UserInfoResponseAssertBuilder extends BaseAssertBuilder {

    private UserInfoResponse response;
    private int status = 200;
    private boolean notNullClaimsPersonalData;
    private boolean notNullClaimsAddressData;
    private String[] claimsPresence;
    private String[] claimsNoPresence;

    public UserInfoResponseAssertBuilder(UserInfoResponse response) {
        this.response = response;
        this.status = 200;
        this.notNullClaimsPersonalData = false;
        this.notNullClaimsAddressData = false;
    }

    public UserInfoResponseAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public UserInfoResponseAssertBuilder notNullClaimsAddressData() {
        this.notNullClaimsAddressData = true;
        return this;
    }

    public UserInfoResponseAssertBuilder notNullClaimsPersonalData() {
        this.notNullClaimsPersonalData = true;
        return this;
    }

    public UserInfoResponseAssertBuilder claimsPresence(String... claimsPresence) {
        if (this.claimsPresence != null) {
            List<String> listClaims = new ArrayList<>();
            listClaims.addAll(Arrays.asList(this.claimsPresence));
            listClaims.addAll(Arrays.asList(claimsPresence));
            this.claimsPresence = listClaims.toArray(new String[0]);
        } else {
            this.claimsPresence = claimsPresence;
        }
        return this;
    }

    public UserInfoResponseAssertBuilder claimsNoPresence(String... claimsNoPresence) {
        if (this.claimsNoPresence != null) {
            List<String> listClaims = new ArrayList<>();
            listClaims.addAll(Arrays.asList(this.claimsNoPresence));
            listClaims.addAll(Arrays.asList(claimsNoPresence));
            this.claimsNoPresence = listClaims.toArray(new String[0]);
        } else {
            this.claimsNoPresence = claimsNoPresence;
        }
        return this;
    }

    @Override
    public void check() {
        assertNotNull(response, "TokenResponse is null");
        if (status == 200) {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));

            //Check Basic Personal Data Claims
            if (notNullClaimsPersonalData) {
                assertNotNull(response.getClaim(JwtClaimName.NAME));
                assertNotNull(response.getClaim(JwtClaimName.GIVEN_NAME));
                assertNotNull(response.getClaim(JwtClaimName.FAMILY_NAME));
                assertNotNull(response.getClaim(JwtClaimName.PICTURE));
                assertNotNull(response.getClaim(JwtClaimName.ZONEINFO));
                assertNotNull(response.getClaim(JwtClaimName.LOCALE));
            }

            //Check Address Data Claims
            if (notNullClaimsAddressData) {
                assertNotNull(response.getClaim(JwtClaimName.ADDRESS));
                assertNotNull(response.getClaim(JwtClaimName.ADDRESS).containsAll(Arrays.asList(
                        JwtClaimName.ADDRESS_STREET_ADDRESS,
                        JwtClaimName.ADDRESS_COUNTRY,
                        JwtClaimName.ADDRESS_LOCALITY,
                        JwtClaimName.ADDRESS_REGION)));
            }
        } else {

        }
        if (claimsPresence != null) {
            for (String claim : claimsPresence) {
                assertNotNull(claim, "Claim name is null");
                assertNotNull(response.getClaim(claim), "UserInfo Claim " + claim + " is not found");
            }
        }

        if (claimsNoPresence != null) {
            for (String claim : claimsNoPresence) {
                assertNotNull(claim, "Claim name is null");
                assertNull(response.getClaim(claim), "UserInfo Claim " + claim + " is found");
            }
        }
    }
}
