package io.jans.as.common.model.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.model.util.Util;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Locale;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ClientSerializationTest {

    @Test
    public void deserialization_whenSerialized_shouldGetCorrectValues() throws IOException {
        Client c = new Client();
        c.setClientName("name");
        c.setClientNameLocalized("myLocalized");
        c.setClientNameLocalized("myLocalized_canada", Locale.CANADA);
        c.setClientNameLocalized("myLocalized_canadaFR", Locale.CANADA_FRENCH);

        final ObjectMapper mapper = Util.createJsonMapper();
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
        final String asJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(c);

        final Client client = mapper.readValue(asJson, Client.class);
        assertNotNull(client);
        assertEquals("myLocalized", client.getClientName());
        assertEquals("myLocalized", client.getClientNameLocalized().getValue());
        assertEquals("myLocalized_canada", client.getClientNameLocalized().getValue(Locale.CANADA));
        assertEquals("myLocalized_canadaFR", client.getClientNameLocalized().getValue(Locale.CANADA_FRENCH));
    }
}
