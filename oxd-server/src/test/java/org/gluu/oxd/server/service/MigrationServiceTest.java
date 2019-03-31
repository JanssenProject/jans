package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.gluu.oxd.server.guice.GuiceModule;
import org.gluu.oxd.server.persistence.PersistenceService;

import java.io.File;

import static org.testng.Assert.assertNotNull;

/**
 * @author yuriyz
 */
@Guice(modules = GuiceModule.class)
public class MigrationServiceTest {

    @Inject
    RpService service;
    @Inject
    PersistenceService persistenceService;
    @Inject
    MigrationService migrationService;

    @BeforeClass
    public void setUp() {
        persistenceService.create();
        service.removeAllRps();
        service.load();
    }

    @AfterSuite
    public void tearDownSuite() {
        try {
            service.removeAllRps();
        } catch (Exception e) {
            // ignore
        }
    }

    @Test(enabled = false)
    public void parse() {
        migrationService.migrateChildren(new File("./oxd-server/src/test/resources/migration"));

        assertNotNull(persistenceService.getRp("0c67e1f0-1fc2-46d6-b237-5e20882ec25c"));
        assertNotNull(persistenceService.getRp("2c65b0da-4bab-4e09-92bb-b028a04a3fe0"));
    }
}
