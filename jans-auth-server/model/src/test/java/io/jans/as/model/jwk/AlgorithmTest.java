package io.jans.as.model.jwk;

import org.json.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class AlgorithmTest {

    @Test
    public void fill_whenCalled_shouldPutCorrectClaims() {
        JSONObject jsonObject = new JSONObject();

        Algorithm.RS256.fill(jsonObject);

        assertEquals(Algorithm.RS256.getOutName(), jsonObject.getString("name"));
        assertEquals(Algorithm.RS256.getDescription(), jsonObject.getString("descr"));
        assertEquals(Algorithm.RS256.getUse().getParamName(), jsonObject.getString("use"));
        assertEquals(Algorithm.RS256.getParamName(), jsonObject.getString("alg"));
    }
}
