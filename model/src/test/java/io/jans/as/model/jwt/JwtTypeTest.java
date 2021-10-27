package io.jans.as.model.jwt;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JwtTypeTest {

    @Test
    public void fromString_withDifferentCasesSensitiveValues_shouldReturnCorrectValue() {
        assertEquals(JwtType.fromString("jwt"), JwtType.JWT);
        assertEquals(JwtType.fromString("Jwt"), JwtType.JWT);
        assertEquals(JwtType.fromString("JWT"), JwtType.JWT);

        assertEquals(JwtType.fromString("dpop+jwt"), JwtType.DPOP_PLUS_JWT);
        assertEquals(JwtType.fromString("Dpop+jwt"), JwtType.DPOP_PLUS_JWT);
        assertEquals(JwtType.fromString("DPOP+JWT"), JwtType.DPOP_PLUS_JWT);
    }

    @Test
    public void fromString_withBlankValue_shouldReturnNull() {
        assertNull(JwtType.fromString(null));
        assertNull(JwtType.fromString(""));
        assertNull(JwtType.fromString("  "));
    }
}
