/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.ClientInfoResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Javier Rojas Blum
 * @version April 6, 2022
 */
public class ClientInfoResponseAssertBuilder extends BaseAssertBuilder {

    private final ClientInfoResponse response;
    private int status;
    private boolean notNullClientInfoClaims;
    private String[] claimsPresence;

    public ClientInfoResponseAssertBuilder(ClientInfoResponse response) {
        this.response = response;
        this.status = 200;
        this.notNullClientInfoClaims = false;
    }

    public ClientInfoResponseAssertBuilder status(int status) {
        this.status = status;
        return this;
    }

    public ClientInfoResponseAssertBuilder notNullClientInfoClaims() {
        this.notNullClientInfoClaims = true;
        return this;
    }

    public ClientInfoResponseAssertBuilder claimsPresence(String... claimsPresence) {
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

    @Override
    public void check() {
        assertNotNull(response, "ClientInfoResponse is null");

        if (status == 200) {
            assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());

            if (notNullClientInfoClaims) {
                assertNotNull(response.getClaim("name"), "Unexpected result: displayName not found");
                assertNotNull(response.getClaim("inum"), "Unexpected result: inum not found");
                assertNotNull(response.getClaim("jansAppType"), "Unexpected result: jansAppTyp not found");
                assertNotNull(response.getClaim("jansIdTknSignedRespAlg"), "Unexpected result: jansIdTknSignedRespAlg not found");
                assertNotNull(response.getClaim("jansRedirectURI"), "Unexpected result: jansRedirectURI not found");
                assertNotNull(response.getClaim("jansScope"), "Unexpected result: jansScope not found");
            }
        }

        if (claimsPresence != null) {
            for (String claim : claimsPresence) {
                assertNotNull(claim, "Claim name is null");
                assertNotNull(response.getClaim(claim), "ClientInfo Claim " + claim + " is not found");
            }
        }
    }
}
