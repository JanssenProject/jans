package org.gluu.oxd.rs.protect.resteasy;

import org.testng.annotations.Test;

import java.io.InputStream;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/04/2016
 */

public class ConfigurationLoaderTest {

    @Test
    public void load() {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("configuration.json");
        Configuration configuration = ConfigurationLoader.loadFromJson(inputStream);

        assertEquals("https://ce-dev.gluu.org", configuration.getOpHost());
        assertEquals("1234-1234", configuration.getUmaPatClientId());
        assertEquals("client_secret", configuration.getUmaPatClientSecret());
    }

}
