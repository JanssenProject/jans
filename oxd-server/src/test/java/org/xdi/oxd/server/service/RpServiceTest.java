package org.xdi.oxd.server.service;

import com.google.inject.Inject;
import org.testng.annotations.*;
import org.xdi.oxd.Tester;
import org.xdi.oxd.server.guice.GuiceModule;
import org.xdi.oxd.server.persistence.PersistenceService;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */
@Guice(modules = GuiceModule.class)
public class RpServiceTest {

    private static ExecutorService EXECUTOR_SERVICE;

    @Inject
    ConfigurationService configurationService;
    @Inject
    RpService service;
    @Inject
    PersistenceService persistenceService;

    @BeforeClass
    public void setUp() {
        Tester.setSystemConfPath();
        configurationService.load();
        persistenceService.create();
        service.removeAllRps();
        service.load();
    }

    @BeforeSuite
    public void setUpSuite() {
        EXECUTOR_SERVICE = Executors.newFixedThreadPool(200);
    }

    @AfterSuite
    public void tearDownSuite() {
        service.removeAllRps();
        persistenceService.destroy();
        EXECUTOR_SERVICE.shutdown();
    }

    @Test
    public void load() throws Exception {
        assertEquals(service.getRps().size(), 1);
    }

    @Test
    public void persist() throws Exception {
        Rp rp = newRp();

        service.create(rp);
        assertEquals(service.getRps().size(), 2);

        rp.setClientName("Updated name");
        service.update(rp);

        assertEquals(service.getRp(rp.getOxdId()).getClientName(), "Updated name");
        assertEquals(persistenceService.getRp(rp.getOxdId()).getClientName(), "Updated name");
    }

    @Test(invocationCount = 10, threadPoolSize = 10)
    public void stressTest() throws IOException {

        final Rp rp = configurationService.defaultRp();
        rp.setOxdId(UUID.randomUUID().toString());
        rp.setPat(UUID.randomUUID().toString());

        service.create(rp);

        for (int i = 0; i < 11; i++) {
            EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        rp.setPat(UUID.randomUUID().toString());
                        service.update(rp);
                        System.out.println("Updated PAT: " + rp.getPat() + ", for site: " + rp.getOxdId());
                    } catch (Throwable e) {
                        throw new AssertionError("Failed to update configuration: " + rp.getOxdId());
                    }
                }
            });
        }
    }

    public Rp newRp() {
        Rp rp = new Rp(configurationService.defaultRp());
        rp.setOxdId(UUID.randomUUID().toString());
        rp.setOpHost("test.gluu.org");
        return rp;
    }

}
