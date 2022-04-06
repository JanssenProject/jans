/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.orm.model.base;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Locale;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Javier Rojas Blum
 * @version April 6, 2022
 */
public class ClientMetadataValueTest {

    ClientMetadataValue clientMetadataValue;

    @BeforeClass
    public void setUp() {
        clientMetadataValue = new ClientMetadataValue();
    }

    @Test
    public void testSetValue() {

        clientMetadataValue.setValue("Client name");
        clientMetadataValue.setValue("Nombre del cliente", new Locale("es"));
        clientMetadataValue.setValue("Nombre del caserito", new Locale("es", "BO"));
        clientMetadataValue.setValue("Client name", Locale.UK);
        clientMetadataValue.setValue("Client name", Locale.CANADA);
        clientMetadataValue.setValue("Client name", Locale.forLanguageTag("en-NZ"));
        clientMetadataValue.setValue("Nom du client", Locale.FRENCH);
        clientMetadataValue.setValue("Nom du client", Locale.CANADA_FRENCH);
        clientMetadataValue.setValue("Nom du client", Locale.FRANCE);
        clientMetadataValue.setValue("クライアント名", Locale.forLanguageTag("ja-Jpan-JP"));
        clientMetadataValue.setValue("カナ姓※", Locale.forLanguageTag("ja-Kana-JP"));
        clientMetadataValue.setValue("漢字姓※", Locale.forLanguageTag("ja-Hani-JP"));

        System.out.println(clientMetadataValue);
        assertEquals(clientMetadataValue.size(), 12);
    }

    @Test(dependsOnMethods = "testSetValue")
    public void testGetValue() {
        assertEquals(clientMetadataValue.getValue(), "Client name");

        for (String key : clientMetadataValue.getLanguageTags()) {
            System.out.println("(" + key + ") " + clientMetadataValue.getValue(key));
            assertNotNull(clientMetadataValue.getValue(key));
        }
    }
}