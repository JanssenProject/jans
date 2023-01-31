package io.jans.as.client.util;

import io.jans.as.model.jwk.KeyOps;
import org.testng.annotations.Test;

import java.util.Calendar;

import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
public class KeyGeneratorContextTest {

    @Test
    public void getExpirationForKeyOps_forConnectKeyOps_shouldReturnPassedExpiration() {
        KeyGeneratorContext context = new KeyGeneratorContext();
        context.setExpirationHours(1);

        final long expirationForKeyOps = context.getExpirationForKeyOps(KeyOps.CONNECT);

        assertTrue(expirationForKeyOps < futureIn2Hours());
    }

    @Test
    public void getExpirationForKeyOps_forSSAKeyOps_shouldReturnExpirationFarInFuture() {
        KeyGeneratorContext context = new KeyGeneratorContext();
        context.setExpirationHours(1);

        final long expirationForKeyOps = context.getExpirationForKeyOps(KeyOps.SSA);

        assertTrue(expirationForKeyOps > futureIn2Hours());
    }

    private long futureIn2Hours() {
        Calendar future2hours = Calendar.getInstance();
        future2hours.add(2, Calendar.HOUR_OF_DAY);
        return future2hours.getTimeInMillis();
    }
}
