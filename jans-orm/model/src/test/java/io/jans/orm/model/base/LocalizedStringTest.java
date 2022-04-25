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
 * @version April 25, 2022
 */
public class LocalizedStringTest {

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

        System.out.println(localizedString);
        assertEquals(localizedString.size(), 12);
    }

    @Test(dependsOnMethods = "testSetValue")
    public void testGetValue() {
        assertEquals(localizedString.getValue(), "Client name");

        for (String key : localizedString.getLanguageTags()) {
            System.out.println("(" + key + ") " + localizedString.getValue(key));
            assertNotNull(localizedString.getValue(key));
        }
    }
}