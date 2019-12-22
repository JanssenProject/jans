package org.gluu.oxd.server.service;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.gluu.oxd.server.guice.GuiceModule;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/09/2016
 */
@Guice(modules = GuiceModule.class)
public class StateServiceTest {

    @Inject
    StateService stateService;

    @Test
    public void generate() {
        generateState();
    }

    private String generateState() {
        String state = stateService.generateState();
        System.out.println(state);
        assertTrue(!Strings.isNullOrEmpty(state));
        return state;
    }
}
