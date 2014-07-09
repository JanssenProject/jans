package org.xdi.oxauth.interop;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

import static org.testng.Assert.assertTrue;

/**
 * OC5:FeatureTest-Support WebFinger Discovery
 *
 * @author Javier Rojas Blum
 * @version 0.9, 06/09/2014
 */
public class SupportWebFingerDiscovery extends BaseTest {

    @Test
    public void supportWebFingerDiscovery() {
        showTitle("OC5:FeatureTest-Support WebFinger Discovery");

        assertTrue(StringUtils.isNotBlank(configurationEndpoint));
    }
}