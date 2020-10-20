package io.jans.ca.server.service;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import io.jans.ca.server.guice.GuiceModule;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

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
