package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;

import static io.jans.as.model.ssa.SsaRequestParam.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class SsaJsonServiceTest {

    @InjectMocks
    private SsaJsonService ssaJsonService;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void jsonObjectToString_jsonObject_validJsonString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a", "val1");
        jsonObject.put("b", "val2");

        String json = ssaJsonService.jsonObjectToString(jsonObject);
        assertNotNull(json, "json response is null");
        JSONObject jsonResponse = new JSONObject(json);
        assertTrue(jsonResponse.has("a"));
        assertTrue(jsonResponse.has("b"));
        assertEquals(jsonResponse.getString("a"), "val1");
        assertEquals(jsonResponse.getString("b"), "val2");
    }

    @Test
    public void jsonArrayToString_jsonArray_validJsonString() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("val1");
        jsonArray.put("val2");

        String json = ssaJsonService.jsonArrayToString(jsonArray);
        assertNotNull(json, "json response is null");
        JSONArray jsonResponse = new JSONArray(json);
        assertEquals(jsonResponse.getString(0), "val1");
        assertEquals(jsonResponse.getString(1), "val2");
    }

    @Test
    public void getJSONArray_ssaList_validJson() {
        Mockito.when(appConfiguration.getIssuer()).thenReturn("https://jans.io");

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);

        Ssa ssa = new Ssa();
        ssa.setId(UUID.randomUUID().toString());
        ssa.setOrgId("1");
        ssa.setExpirationDate(calendar.getTime());
        ssa.setDescription("Test description");
        ssa.getAttributes().setSoftwareId("scan-api-test");
        ssa.getAttributes().setSoftwareRoles(Collections.singletonList("password"));
        ssa.getAttributes().setGrantTypes(Collections.singletonList("client_credentials"));
        ssa.getAttributes().setOneTimeUse(true);
        ssa.getAttributes().setRotateSsa(true);
        ssa.setCreatorId("test@localhost");

        JSONArray jsonArray = ssaJsonService.getJSONArray(Collections.singletonList(ssa));
        assertNotNull(jsonArray, "jsonObject response is null");
        assertEquals(jsonArray.length(), 1);

        JSONObject jsonObject = jsonArray.getJSONObject(0);
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has(SSA.getName()));
        assertTrue(jsonObject.has(CREATED_AT.getName()));
        assertEquals(jsonObject.get(CREATED_AT.getName()), DateUtil.dateToUnixEpoch(ssa.getCreationDate()));
        assertTrue(jsonObject.has(EXPIRATION.getName()));
        assertEquals(jsonObject.get(EXPIRATION.getName()), DateUtil.dateToUnixEpoch(ssa.getExpirationDate()));
        assertTrue(jsonObject.has(ISSUER.getName()));

        JSONObject ssaJsonObject = jsonObject.getJSONObject(SSA.getName());
        assertNotNull(ssaJsonObject);
        assertTrue(ssaJsonObject.has(ORG_ID.getName()));
        assertEquals(ssaJsonObject.get(ORG_ID.getName()), Long.parseLong(ssa.getOrgId()));
        assertTrue(ssaJsonObject.has(SOFTWARE_ID.getName()));
        assertEquals(ssaJsonObject.get(SOFTWARE_ID.getName()), ssa.getAttributes().getSoftwareId());
        assertTrue(ssaJsonObject.has(SOFTWARE_ROLES.getName()));
        assertEquals(ssaJsonObject.get(SOFTWARE_ROLES.getName()), ssa.getAttributes().getSoftwareRoles());
        assertTrue(ssaJsonObject.has(GRANT_TYPES.getName()));
        assertEquals(ssaJsonObject.get(GRANT_TYPES.getName()), ssa.getAttributes().getGrantTypes());
        assertTrue(ssaJsonObject.has(EXP.getName()));
        assertEquals(ssaJsonObject.get(EXP.getName()), DateUtil.dateToUnixEpoch(ssa.getExpirationDate()));
        assertTrue(ssaJsonObject.has(JTI.getName()));
        assertEquals(ssaJsonObject.get(JTI.getName()), ssa.getId());
        assertTrue(ssaJsonObject.has(IAT.getName()));
        assertEquals(ssaJsonObject.get(IAT.getName()), DateUtil.dateToUnixEpoch(ssa.getCreationDate()));
        assertTrue(ssaJsonObject.has(ISS.getName()));
        assertEquals(ssaJsonObject.get(ISS.getName()), appConfiguration.getIssuer());
    }

    @Test
    public void getJSONArray_emptySsaList_validEmptyJson() {
        JSONArray jsonArray = ssaJsonService.getJSONArray(Collections.emptyList());
        assertNotNull(jsonArray, "jsonArray response is null");
        assertEquals(jsonArray.length(), 0);
    }

    @Test
    public void getJSONArray_nullSsaList_validEmptyJson() {
        JSONArray jsonArray = ssaJsonService.getJSONArray(null);
        assertNotNull(jsonArray, "jsonArray response is null");
        assertEquals(jsonArray.length(), 0);
    }

    @Test
    public void getJSONObject_jwt_validJsonObject() {
        String jwt = "eyJraWQiOiIxOGZlOTQ2YS1mMjkyLTQ1MTgtYWRmYi00ZTA1ZDAzODM0MDBfc2lnX3JzNTEyIiwidHlwIjoiand0IiwiYWxnIjoiUlM1MTIiLCJjdXN0b21faGVhZGVyX25hbWUiOiJjdXN0b21faGVhZGVyX3ZhbHVlIn0.eyJzb2Z0d2FyZV9pZCI6ImdsdXUtc2Nhbi1hcGkiLCJncmFudF90eXBlcyI6WyJjbGllbnRfY3JlZGVudGlhbHMiXSwib3JnX2lkIjoxLCJpc3MiOiJodHRwczovL2phbnMubG9jYWxob3N0Iiwic29mdHdhcmVfcm9sZXMiOlsicGFzc3d1cmQiXSwiZXhwIjoxNjY2MTM3MjUzLCJpYXQiOjE2NjM1NDUyNTMsImN1c3RvbV9jbGFpbV9uYW1lIjoiY3VzdG9tX2NsYWltX3ZhbHVlIiwianRpIjoiY2M4OTQ0MjItMzRlOC00MzUxLTkzZWEtMDkzYmEzN2RjOTIyIn0.02GCtFMpX_srmQs5neNv92Du4bsHsxzROQpx4Zf8XMnv7F3AYw_czkBrGsVHoJLRFttl4esHgfY4vhCp9uYhNxaM6C8tscpIT7c26C2F378inEACC_gh3_v-AEogH_KhUHeDxyD9ZVCWVSHXoc-jN8BAPqIqyK1ndmWO-l8cFsSyjuCJYTJDcYa-E1lsRlUjHQaLQXIRaTWm_rA-GFeaacQQ6AXyIwVNO5jcMXpgS0p0QY9F1jPSpEus44inQ88NInYHHzPZKDgjeP8py6K9TFVU_ABh4QR6JI62ZNnddEt676I2AYuvekV0PtJ8hTUJKPcBBiAen05w2abwh3rHRg";

        JSONObject jsonObject = ssaJsonService.getJSONObject(jwt);
        assertNotNull(jsonObject, "jsonObject response is null");
        assertTrue(jsonObject.has(SSA.getName()));
        assertEquals(jsonObject.get(SSA.getName()), jwt);
    }

    @Test
    public void getJSONObject_jwtNull_emptyJsonObject() {
        String jwt = null;

        JSONObject jsonObject = ssaJsonService.getJSONObject(jwt);
        assertNotNull(jsonObject, "jsonObject response is null");
        assertTrue(jsonObject.isEmpty());
    }

    @Test
    public void getJSONObject_jwtBlank_validWithBlankValue() {
        String jwt = "";

        JSONObject jsonObject = ssaJsonService.getJSONObject(jwt);
        assertNotNull(jsonObject, "jsonObject response is null");
        assertTrue(jsonObject.has(SSA.getName()));
        assertEquals(jsonObject.get(SSA.getName()), "");
    }
}