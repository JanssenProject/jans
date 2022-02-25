package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.UserInfoResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.jwt.JwtClaimName;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class UserInfoResponseAssertBuilder extends AssertBuilder {

    private UserInfoResponse response;
    private int status = 200;
    private boolean notNullClaimIssuer;
    private boolean notNullClaimAudience;
    private boolean notNullClaimEmail;
    private boolean notNullClaimsPersonalData;
    private boolean notNullClaimsAddressData;
    private boolean notNullClaimAddressCountry;
    private boolean notNullClaimAddressStreet;

    public UserInfoResponseAssertBuilder(UserInfoResponse response) {
        this.response = response;
        this.status = 200;
        this.notNullClaimIssuer = false;
        this.notNullClaimAudience = false;
        this.notNullClaimEmail = false;
        this.notNullClaimsPersonalData = false;
        this.notNullClaimsAddressData = false;
        this.notNullClaimAddressCountry = false;
        this.notNullClaimAddressStreet = false;
    }

    public UserInfoResponseAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public UserInfoResponseAssertBuilder notNullClaimIssuer() {
        this.notNullClaimIssuer = true;
        return this;
    }

    public UserInfoResponseAssertBuilder notNullClaimAudience() {
        this.notNullClaimAudience = true;
        return this;
    }

    public UserInfoResponseAssertBuilder notNullClaimEmail() {
        this.notNullClaimEmail = true;
        return this;
    }

    public UserInfoResponseAssertBuilder notNullClaimAddressCountry() {
        this.notNullClaimAddressCountry = true;
        return this;
    }

    public UserInfoResponseAssertBuilder notNullClaimAddressStreet() {
        this.notNullClaimAddressStreet = true;
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


    @Override
    public void checkAsserts() {
        assertNotNull(response, "TokenResponse is null");
        if (status == 200) {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
            assertNotNull(response.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            if (notNullClaimIssuer) {
                assertNotNull(response.getClaim(JwtClaimName.ISSUER));
            }
            if (notNullClaimAudience) {
                assertNotNull(response.getClaim(JwtClaimName.AUDIENCE));
            }

            //Check Basic Personal Data Claims
            if (notNullClaimsPersonalData) {
                assertNotNull(response.getClaim(JwtClaimName.NAME));
                assertNotNull(response.getClaim(JwtClaimName.GIVEN_NAME));
                assertNotNull(response.getClaim(JwtClaimName.FAMILY_NAME));
                assertNotNull(response.getClaim(JwtClaimName.PICTURE));
                if (notNullClaimEmail) {
                    assertNotNull(response.getClaim(JwtClaimName.EMAIL));
                }
                assertNotNull(response.getClaim(JwtClaimName.ZONEINFO));
                assertNotNull(response.getClaim(JwtClaimName.LOCALE));
            }

            //Check Address Data Claims
            if (notNullClaimAddressStreet) {
                assertNotNull(response.getClaim(JwtClaimName.ADDRESS_STREET_ADDRESS));
            }
            if (notNullClaimAddressCountry) {
                assertNotNull(response.getClaim(JwtClaimName.ADDRESS_COUNTRY));
            }
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
    }
}
