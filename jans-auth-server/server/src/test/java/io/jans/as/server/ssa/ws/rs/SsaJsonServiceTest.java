package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.ssa.Ssa;
import org.json.JSONObject;
import org.mockito.InjectMocks;
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
    public void getJSONObject_ssa_validJsonObject() {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);

        Ssa ssa = new Ssa();
        ssa.setId(UUID.randomUUID().toString());
        ssa.setOrgId(1L);
        ssa.setExpiration(calendar.getTime());
        ssa.setDescription("Test description");
        ssa.setSoftwareId("scan-api-test");
        ssa.setSoftwareRoles(Collections.singletonList("passwurd"));
        ssa.setGrantTypes(Collections.singletonList("client_credentials"));
        ssa.setOneTimeUse(true);
        ssa.setRotateSsa(true);

        JSONObject jsonObject = ssaJsonService.getJSONObject(ssa);
        assertNotNull(jsonObject, "jsonObject response is null");
        assertTrue(jsonObject.has(ORG_ID.toString()));
        assertEquals(jsonObject.get(ORG_ID.toString()), ssa.getOrgId());
        assertTrue(jsonObject.has(EXPIRATION.toString()));
        assertEquals(jsonObject.get(EXPIRATION.toString()), ssa.getExpiration());
        assertTrue(jsonObject.has(DESCRIPTION.toString()));
        assertEquals(jsonObject.get(DESCRIPTION.toString()), ssa.getDescription());
        assertTrue(jsonObject.has(SOFTWARE_ID.toString()));
        assertEquals(jsonObject.get(SOFTWARE_ID.toString()), ssa.getSoftwareId());
        assertTrue(jsonObject.has(SOFTWARE_ROLES.toString()));
        assertEquals(jsonObject.get(SOFTWARE_ROLES.toString()), ssa.getSoftwareRoles());
        assertTrue(jsonObject.has(GRANT_TYPES.toString()));
        assertEquals(jsonObject.get(GRANT_TYPES.toString()), ssa.getGrantTypes());
        assertTrue(jsonObject.has(ONE_TIME_USE.toString()));
        assertEquals(jsonObject.get(ONE_TIME_USE.toString()), ssa.getOneTimeUse());
        assertTrue(jsonObject.has(ROTATE_SSA.toString()));
        assertEquals(jsonObject.get(ROTATE_SSA.toString()), ssa.getRotateSsa());
    }

    @Test
    public void getJSONObject_jwt_validJsonObject() {
        String jwt = "eyJraWQiOiIxOGZlOTQ2YS1mMjkyLTQ1MTgtYWRmYi00ZTA1ZDAzODM0MDBfc2lnX3JzNTEyIiwidHlwIjoiand0IiwiYWxnIjoiUlM1MTIiLCJjdXN0b21faGVhZGVyX25hbWUiOiJjdXN0b21faGVhZGVyX3ZhbHVlIn0.eyJzb2Z0d2FyZV9pZCI6ImdsdXUtc2Nhbi1hcGkiLCJncmFudF90eXBlcyI6WyJjbGllbnRfY3JlZGVudGlhbHMiXSwib3JnX2lkIjoxLCJpc3MiOiJodHRwczovL2phbnMubG9jYWxob3N0Iiwic29mdHdhcmVfcm9sZXMiOlsicGFzc3d1cmQiXSwiZXhwIjoxNjY2MTM3MjUzLCJpYXQiOjE2NjM1NDUyNTMsImN1c3RvbV9jbGFpbV9uYW1lIjoiY3VzdG9tX2NsYWltX3ZhbHVlIiwianRpIjoiY2M4OTQ0MjItMzRlOC00MzUxLTkzZWEtMDkzYmEzN2RjOTIyIn0.02GCtFMpX_srmQs5neNv92Du4bsHsxzROQpx4Zf8XMnv7F3AYw_czkBrGsVHoJLRFttl4esHgfY4vhCp9uYhNxaM6C8tscpIT7c26C2F378inEACC_gh3_v-AEogH_KhUHeDxyD9ZVCWVSHXoc-jN8BAPqIqyK1ndmWO-l8cFsSyjuCJYTJDcYa-E1lsRlUjHQaLQXIRaTWm_rA-GFeaacQQ6AXyIwVNO5jcMXpgS0p0QY9F1jPSpEus44inQ88NInYHHzPZKDgjeP8py6K9TFVU_ABh4QR6JI62ZNnddEt676I2AYuvekV0PtJ8hTUJKPcBBiAen05w2abwh3rHRg";

        JSONObject jsonObject = ssaJsonService.getJSONObject(jwt);
        assertNotNull(jsonObject, "jsonObject response is null");
        assertTrue(jsonObject.has("ssa"));
        assertEquals(jsonObject.get("ssa"), jwt);
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
        assertTrue(jsonObject.has("ssa"));
        assertEquals(jsonObject.get("ssa"), "");
    }
}