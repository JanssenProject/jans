package io.jans.as.server.servlet;

import io.jans.as.model.configuration.AppConfiguration;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Z
 */
public class OpenIdConfigurationTest {

    @Test
    public void filterOutKeys_withBlankValues_shouldRemoveKeys() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key1", "");
        jsonObject.put("key2", "value2");
        jsonObject.put("key3", "  ");

        OpenIdConfiguration.filterOutKeys(jsonObject, new AppConfiguration());

        assertEquals("value2", jsonObject.get("key2"));
        assertFalse(jsonObject.has("key1"));
        assertFalse(jsonObject.has("key3"));
    }

    @Test
    public void filterOutKeys_withBlankValuesAndAllowedBlankValuesInConfig_shouldNotRemoveKeys() {
        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setAllowBlankValuesInDiscoveryResponse(true);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key1", "");
        jsonObject.put("key2", "value2");
        jsonObject.put("key3", "  ");

        OpenIdConfiguration.filterOutKeys(jsonObject, appConfiguration);

        assertEquals("value2", jsonObject.get("key2"));
        assertTrue(jsonObject.has("key1"));
        assertTrue(jsonObject.has("key3"));
    }

    @Test
    public void getAcrValuesList_whenCalled_shouldContainInternalAuthnAlias() {
        final List<String> acrValuesList = OpenIdConfiguration.getAcrValuesList(new ArrayList<>());
        assertTrue(acrValuesList.contains("simple_password_auth"));
    }

    @Test
    public void filterOutKeys_whenKeyIsInDentiedList_mustRemoveThemFromJson() {
        AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.getDiscoveryDenyKeys().add("test");

        JSONObject json = new JSONObject("{\"test\": 1}");
        assertTrue(json.has("test"));

        OpenIdConfiguration.filterOutKeys(json, appConfiguration);
        assertFalse(json.has("test"));
    }

    @Test
    public void filterOutKeys_whenKeyIsNotInDentiedList_mustNotRemoveThemFromJson() {
        AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.getDiscoveryDenyKeys().add("testX");

        JSONObject json = new JSONObject("{\"test\": 1}");
        assertTrue(json.has("test"));

        OpenIdConfiguration.filterOutKeys(json, appConfiguration);
        assertTrue(json.has("test"));
    }
}
