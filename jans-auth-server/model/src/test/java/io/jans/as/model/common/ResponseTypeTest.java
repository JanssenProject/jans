package io.jans.as.model.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ResponseTypeTest {


    @Test
    public void isImplicitFlow_withNull_shouldReturnFalseWithoutException() {
        assertFalse(ResponseType.isImplicitFlow(null));
    }

    @Test
    public void isImplicitFlow_withBlankValue_shouldReturnFalse() {
        assertFalse(ResponseType.isImplicitFlow(""));
    }

    @Test
    public void isImplicitFlow_withUnknownValue_shouldReturnFalse() {
        assertFalse(ResponseType.isImplicitFlow("dfs"));
    }

    @Test
    public void isImplicitFlow_withTokenAndIdTokenValue_shouldReturnTrue() {
        assertTrue(ResponseType.isImplicitFlow("token id_token"));
    }
}
