/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/10/2013
 */

public class TestUtil {

    private static final Logger LOG = Logger.getLogger(TestUtil.class);

    private TestUtil() {
    }

    public static void assertNotBlank(String p_str) {
        assertTrue(StringUtils.isNotBlank(p_str));
    }

    public static void assertErrorResponse(String p_entity) {
        assertNotNull(p_entity, "Unexpected result: " + p_entity);
        try {
            JSONObject jsonObj = new JSONObject(p_entity);
            assertTrue(jsonObj.has("error"), "The error type is null");
            assertTrue(jsonObj.has("error_description"), "The error description is null");
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
            fail(e.getMessage() + "\nResponse was: " + p_entity);
        }
    }
}
