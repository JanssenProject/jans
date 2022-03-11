package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.par.ParResponse;
import org.apache.commons.lang3.StringUtils;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ParResponseAssertBuilder extends BaseAssertBuilder {

    ParResponse response;

    public ParResponseAssertBuilder(ParResponse response) {
        this.response = response;
    }

    @Override
    public void check() {
        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getRequestUri()));
        assertNotNull(response.getExpiresIn());
    }
}
