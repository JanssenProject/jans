package io.jans.as.server.util;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.ArrayUtils;
import org.json.JSONObject;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
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
