package io.swagger.client.api;

import io.swagger.client.model.GetJwksParams;
import io.swagger.client.model.GetJwksResponse;
import io.swagger.client.model.GetRpJwksResponse;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static io.swagger.client.api.Tester.api;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class GetRpJwksTest {

    @Test
    @Parameters({"opHost"})
    public void test(String opHost) throws Exception {

        final DevelopersApi client = api();
        final GetRpJwksResponse response = client.getRpJwks();

        assertNotNull(response);
        assertNotNull(response.getKeys());
        assertFalse(response.getKeys().isEmpty());

    }
}
