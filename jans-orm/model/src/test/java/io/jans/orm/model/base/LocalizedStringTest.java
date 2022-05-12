/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.orm.model.base;

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version April 26, 2022
 */
public class LocalizedStringTest {

    final String[] LANGUAGE_TAGS = new String[]{
            "", "es", "es-BO",
            "en-GB", "en-CA", "en-NZ",
            "fr", "fr-FR", "fr-CA",
            "ja-Jpan-JP", "ja-Kana-JP", "ja-Hani-JP"
    };

    final String[] LOCALIZED_VALUES = new String[]{
            "Client name", "Nombre del cliente", "Nombre del cliente",
            "Client name", "Client name", "Client name",
            "Nom du client", "Nom du client", "Nom du client",
            "クライアント名", "カナ姓※", "漢字姓※"
    };
    final String[] CLAIM_NAMES = new String[]{
            "client_name", "client_name#es", "client_name#es-BO",
            "client_name#en-GB", "client_name#en-CA", "client_name#en-NZ",
            "client_name#fr", "client_name#fr-FR", "client_name#fr-CA",
            "client_name#ja-Jpan-JP", "client_name#ja-Kana-JP", "client_name#ja-Hani-JP"
    };

    final String REQUEST_JSON = "{\n" +
            "  \"redirect_uris\" : [ \"https://gluu/jans-auth-rp/home.htm\", \"https://client.example.com/cb\", \"https://client.example.com/cb1\", \"https://client.example.com/cb2\" ],\n" +
            "  \"sector_identifier_uri\" : \"https://gluu/jans-auth/sectoridentifier/a55ede29-8f5a-461d-b06e-76caee8d40b5\",\n" +
            "  \"response_types\" : [ \"code\", \"token\", \"id_token\" ],\n" +
            "  \"client_uri\" : \"https://client-home-page/index.htm\",\n" +
            "  \"application_type\" : \"web\",\n" +
            "  \"scope\" : \"openid profile address email phone user_name clientinfo\",\n" +
            "  \"subject_type\" : \"pairwise\",\n" +
            "  \"client_name\" : \"Client name\",\n" +
            "  \"client_name#es\" : \"Nombre del cliente\",\n" +
            "  \"client_name#es-BO\" : \"Nombre del cliente\",\n" +
            "  \"client_name#en-GB\" : \"Client name\",\n" +
            "  \"client_name#en-CA\" : \"Client name\",\n" +
            "  \"client_name#en-NZ\" : \"Client name\",\n" +
            "  \"client_name#fr\" : \"Nom du client\",\n" +
            "  \"client_name#fr-FR\" : \"Nom du client\",\n" +
            "  \"client_name#fr-CA\" : \"Nom du client\",\n" +
            "  \"client_name#ja-Jpan-JP\" : \"クライアント名\",\n" +
            "  \"client_name#ja-Kana-JP\" : \"カナ姓※\",\n" +
            "  \"client_name#ja-Hani-JP\" : \"漢字姓※\"\n" +
            "}";

    LocalizedString localizedString;

    @BeforeClass
    public void setUp() {
        localizedString = new LocalizedString();
    }

    @Test
    public void testSetValue() {
        localizedString.setValue("Client name");
        localizedString.setValue("Nombre del cliente", new Locale("es"));
        localizedString.setValue("Nombre del cliente", new Locale("es", "BO"));
        localizedString.setValue("Client name", Locale.UK);
        localizedString.setValue("Client name", Locale.CANADA);
        localizedString.setValue("Client name", Locale.forLanguageTag("en-NZ"));
        localizedString.setValue("Nom du client", Locale.FRENCH);
        localizedString.setValue("Nom du client", Locale.CANADA_FRENCH);
        localizedString.setValue("Nom du client", Locale.FRANCE);
        localizedString.setValue("クライアント名", Locale.forLanguageTag("ja-Jpan-JP"));
        localizedString.setValue("カナ姓※", Locale.forLanguageTag("ja-Kana-JP"));
        localizedString.setValue("漢字姓※", Locale.forLanguageTag("ja-Hani-JP"));

        assertEquals(localizedString.size(), LANGUAGE_TAGS.length);
        assertTrue(localizedString.getLanguageTags().containsAll(Arrays.asList(LANGUAGE_TAGS)));
        for (int i = 0; i < LANGUAGE_TAGS.length; i++) {
            assertEquals(localizedString.getValue(LANGUAGE_TAGS[i]), LOCALIZED_VALUES[i]);
        }
    }

    @Test(dependsOnMethods = "testSetValue")
    public void testGetValue() {
        assertEquals(localizedString.getValue(), "Client name");
    }

    @Test(dependsOnMethods = "testSetValue")
    public void addToMap() {
        Map<String, Object> map = new HashMap<>();

        localizedString.addToMap(map, "client_name");

        assertEquals(map.size(), CLAIM_NAMES.length);
        for (int i = 0; i < CLAIM_NAMES.length; i++) {
            assertTrue(map.containsKey(CLAIM_NAMES[i]));
            assertEquals(map.get(CLAIM_NAMES[i]), LOCALIZED_VALUES[i]);
        }
    }

    @Test(dependsOnMethods = "testSetValue")
    public void addToJSON() {
        JSONObject jsonObject = new JSONObject();

        localizedString.addToJSON(jsonObject, "client_name");

        assertEquals(jsonObject.keySet().size(), CLAIM_NAMES.length);
        for (int i = 0; i < CLAIM_NAMES.length; i++) {
            assertTrue(jsonObject.keySet().contains(CLAIM_NAMES[i]));
            assertEquals(jsonObject.get(CLAIM_NAMES[i]), LOCALIZED_VALUES[i]);
        }
    }

    @Test
    public void fromJson() {
        JSONObject jsonObject = new JSONObject(REQUEST_JSON);
        LocalizedString myLocalizedString = new LocalizedString();

        LocalizedString.fromJson(jsonObject, "client_name", (value, locale) -> {
            myLocalizedString.setValue(value, locale);
            return null;
        });

        assertEquals(myLocalizedString.size(), LANGUAGE_TAGS.length);
        assertTrue(myLocalizedString.getLanguageTags().containsAll(Arrays.asList(LANGUAGE_TAGS)));
        for (int i = 0; i < LANGUAGE_TAGS.length; i++) {
            assertEquals(myLocalizedString.getValue(LANGUAGE_TAGS[i]), LOCALIZED_VALUES[i]);
        }
    }
}