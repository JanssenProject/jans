package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.ssa.get.SsaGetJson;
import io.jans.as.client.ssa.get.SsaGetResponse;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static io.jans.as.model.ssa.SsaRequestParam.*;
import static org.testng.Assert.*;

public class SsaGetAssertBuilder extends BaseAssertBuilder {

    private final SsaGetResponse response;

    private Integer ssaListSize;

    private List<String> jtiList;

    public SsaGetAssertBuilder(SsaGetResponse response) {
        this.response = response;
    }

    public SsaGetAssertBuilder ssaListSize(Integer ssaListSize) {
        this.ssaListSize = ssaListSize;
        return this;
    }

    public SsaGetAssertBuilder jtiList(List<String> jtiList) {
        this.jtiList = jtiList;
        return this;
    }

    @Override
    public void check() {
        assertNotNull(response, "SsaResponse is null");
        assertEquals(response.getStatus(), HttpStatus.SC_OK, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getSsaList(), "The ssaList is null");
        if (jtiList != null && !jtiList.isEmpty()) {
            response.setSsaList(filterSsaList(response.getSsaList(), jtiList));
        }

        if (ssaListSize != null) {
            assertEquals(response.getSsaList().size(), ssaListSize.intValue());
            if (ssaListSize > 0) {
                response.getSsaList().forEach(ssaGetJson -> {
                    JSONObject ssaWrapper = ssaGetJson.getJsonObject();
                    assertNotNull(ssaWrapper);
                    assertTrue(ssaWrapper.has(SSA.getName()));
                    assertTrue(ssaWrapper.has(CREATED_AT.getName()));
                    assertTrue(ssaWrapper.has(EXPIRATION.getName()));
                    assertTrue(ssaWrapper.has(ISSUER.getName()));

                    JSONObject ssaJson = ssaWrapper.getJSONObject(SSA.getName());
                    assertNotNull(ssaJson);
                    assertTrue(ssaJson.has(ISS.getName()));
                    assertTrue(ssaJson.has(IAT.getName()));
                    assertTrue(ssaJson.has(JTI.getName()));
                    assertTrue(ssaJson.has(SOFTWARE_ID.getName()));
                    assertTrue(ssaJson.has(SOFTWARE_ROLES.getName()));
                    assertTrue(ssaJson.has(GRANT_TYPES.getName()));
                    assertTrue(ssaJson.has(EXP.getName()));
                });
            }
        }
    }

    private List<SsaGetJson> filterSsaList(List<SsaGetJson> ssaGetJsonList, List<String> jtiList) {
        List<SsaGetJson> ssaList = new ArrayList<>();
        jtiList.forEach(s -> ssaGetJsonList.stream()
                .filter(ssaGetJson -> ssaGetJson.getJti().equals(s))
                .findFirst().map(ssaList::add));
        return ssaList;
    }
}
