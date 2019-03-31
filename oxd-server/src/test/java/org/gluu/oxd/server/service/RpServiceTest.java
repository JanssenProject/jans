package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import org.testng.annotations.*;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.guice.GuiceModule;
import org.gluu.oxd.server.persistence.PersistenceService;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

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
        assertEquals(service.getRps().size(), 1);

        rp.setClientName("Updated name");
        service.update(rp);

        assertEquals(service.getRp(rp.getOxdId()).getClientName(), "Updated name");
        assertEquals(persistenceService.getRp(rp.getOxdId()).getClientName(), "Updated name");
    }

    @Test
    public void remove() throws Exception {
        Rp rp = newRp();

        service.create(rp);
        assertNotNull(service.getRp(rp.getOxdId()));

        rp.setClientName("Updated name");
        service.update(rp);

        assertEquals(service.getRp(rp.getOxdId()).getClientName(), "Updated name");
        assertEquals(persistenceService.getRp(rp.getOxdId()).getClientName(), "Updated name");

        service.remove(rp.getOxdId());
        try {
            service.getRp(rp.getOxdId());
            throw new AssertionError("RP is not removed.");
        } catch (HttpException e) {
            assertEquals(e.getCode(), ErrorResponseCode.INVALID_OXD_ID);
        }
    }

    @Test(invocationCount = 10, threadPoolSize = 10, enabled = false)
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
