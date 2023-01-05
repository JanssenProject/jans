/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.test;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/10/2013
 */

public class TestUtil {

    private static final Logger LOG = Logger.getLogger(TestUtil.class);

    private TestUtil() {
    }

    public static void assertNotBlank(String str) {
        assertTrue(StringUtils.isNotBlank(str));
    }

    public static void assertErrorResponse(String entity) {
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("error"), "The error type is null");
            assertTrue(jsonObj.has("error_description"), "The error description is null");
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }
}
