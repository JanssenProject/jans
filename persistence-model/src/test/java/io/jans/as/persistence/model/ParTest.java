package io.jans.as.persistence.model;

import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ParTest {

    @Test
    public void isExpired_whenExpirationDateIsNull_shouldReturnTrue() {
        Par par = new Par();
        assertTrue(par.isExpired());
    }

    @Test
    public void isExpired_whenExpirationDateIsInFuture_shouldReturnFalse() {
        Par par = new Par();

        Date expInFuture = new Date(System.currentTimeMillis() + 100000);
        par.setExpirationDate(expInFuture);

        assertFalse(par.isExpired());
    }

    @Test
    public void isExpired_whenExpirationDateIsInPast_shouldReturnTrue() {
        Par par = new Par();

        Date expInPast = new Date(System.currentTimeMillis() - 100000);
        par.setExpirationDate(expInPast);

        assertTrue(par.isExpired());
    }

}
