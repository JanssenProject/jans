package io.swagger.client.api;

import static org.testng.Assert.assertNotNull;

import com.google.common.collect.Lists;
import io.swagger.client.ApiException;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponseData;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;



/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 * @version 9/17/2018
 */

public class GetClientTokenTest {

    @Parameters({ "opHost"})
    @Test
    public void getClientToken(String opHost) throws ApiException {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(opHost);
        params.setScope(Lists.newArrayList("openid","oxd"));
        params.setClientId(Tester.getSetupData().getClientId());
        params.setClientSecret(Tester.getSetupData().getClientSecret());

        GetClientTokenResponseData resp = Tester.api().getClientToken(params).getData();
        assertNotNull(resp);
        Tester.notEmpty(resp.getAccessToken());

    }
}
