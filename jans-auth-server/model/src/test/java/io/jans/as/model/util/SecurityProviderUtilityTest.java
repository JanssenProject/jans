package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import io.jans.util.security.SecurityProviderUtility;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class SecurityProviderUtilityTest extends BaseTest {

    @Test
    public void installBCProvider_silent_validInstance() {
        showTitle("installBCProvider_silent_validInstance");
        SecurityProviderUtility.installBCProvider(true);
        assertNotNull(SecurityProviderUtility.getBCProvider());
        assertNotNull(SecurityProviderUtility.getSecurityMode());
    }

    @Test
    public void installBCProvider_Nosilent_validInstance() {
        showTitle("installBCProvider_Nosilent_validInstance");
        SecurityProviderUtility.installBCProvider();
        assertNotNull(SecurityProviderUtility.getBCProvider());
        assertNotNull(SecurityProviderUtility.getSecurityMode());
    }
}
