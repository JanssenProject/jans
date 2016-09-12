package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import junit.framework.Assert;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.xdi.oxd.web.TestAppModule;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/09/2016
 */
@Guice(modules = TestAppModule.class)
public class StateServiceTest {

    @Inject
    StateService stateService;

    @Test
    public void generate() throws Exception {
        generateState();
    }

    private String generateState() {
        String state = stateService.generateState();
        System.out.println(state);
        Assert.assertTrue(!Strings.isNullOrEmpty(state));
        return state;
    }
}
