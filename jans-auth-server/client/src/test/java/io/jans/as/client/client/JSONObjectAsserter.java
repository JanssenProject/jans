/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.ArrayUtils;
import org.json.JSONObject;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author yuriyz
 */
public class JSONObjectAsserter {

    private final JSONObject json;

    private JSONObjectAsserter(JSONObject json) {
        Preconditions.checkNotNull(json);
        this.json = json;
    }

    public static JSONObjectAsserter of(JSONObject json) {
        assertNotNull(json);
        return new JSONObjectAsserter(json);
    }

    public JSONObjectAsserter hasKeys(String... keys) {
        if (!ArrayUtils.isEmpty(keys)) {
            for (String key : keys) {
                assertTrue(json.has(key));
            }
        }

        return this;
    }

    public JSONObject getJson() {
        return json;
    }
}
