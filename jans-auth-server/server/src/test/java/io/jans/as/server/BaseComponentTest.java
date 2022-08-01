/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server;

import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.server.idgen.ws.rs.InumGenerator;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.service.CleanerTimer;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.uma.service.UmaPctService;
import io.jans.as.server.uma.service.UmaPermissionService;
import io.jans.as.server.uma.service.UmaResourceService;
import io.jans.as.server.uma.service.UmaRptService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.CacheService;

import static io.jans.as.server.service.TestInjectionService.HM_TEST_INJECTOR;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 15/10/2012
 */

public abstract class BaseComponentTest extends BaseTest {

    public static void sleepSeconds(int p_seconds) {
        try {
            Thread.sleep(p_seconds * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static InumGenerator getInumGenerator() {
        return (InumGenerator) HM_TEST_INJECTOR.get("InumGenerator");
    }

    public static StaticConfiguration getStaticConfiguration() {
        return (StaticConfiguration) HM_TEST_INJECTOR.get("StaticConfiguration");
    }

    public ClientService getClientService() {
        return (ClientService) HM_TEST_INJECTOR.get("ClientService");
    }

    public InumService getInumService() {
        return (InumService) HM_TEST_INJECTOR.get("InumService");
    }

    public CleanerTimer getCleanerTimer() {
        return (CleanerTimer) HM_TEST_INJECTOR.get("CleanerTimer");
    }

    public CacheService getCacheService() {
        return (CacheService) HM_TEST_INJECTOR.get("CacheService");
    }

    public UmaRptService getUmaRptService() {
        return (UmaRptService) HM_TEST_INJECTOR.get("UmaRptService");
    }

    public UmaResourceService getUmaResourceService() {
        return (UmaResourceService) HM_TEST_INJECTOR.get("UmaResourceService");
    }

    public UmaPermissionService getUmaPermissionService() {
        return (UmaPermissionService) HM_TEST_INJECTOR.get("UmaPermissionService");
    }

    public UmaPctService getUmaPctService() {
        return (UmaPctService) HM_TEST_INJECTOR.get("UmaPctService");
    }

    public AuthorizationGrantList getAuthorizationGrantList() {
        return (AuthorizationGrantList) HM_TEST_INJECTOR.get("AuthorizationGrantList");
    }

    public GrantService getGrantService() {
        return (GrantService) HM_TEST_INJECTOR.get("GrantService");
    }

    public EncryptionService getEncryptionService() {
        return (EncryptionService) HM_TEST_INJECTOR.get("EncryptionService");
    }

    public ConfigurationFactory getConfigurationFactory() {
        return (ConfigurationFactory) HM_TEST_INJECTOR.get("ConfigurationFactory");
    }

    public AbstractCryptoProvider getAbstractCryptoProvider() {
        return (AbstractCryptoProvider) HM_TEST_INJECTOR.get("AbstractCryptoProvider");
    }

    public SessionIdService getSessionIdService() {
        return (SessionIdService) HM_TEST_INJECTOR.get("SessionIdService");
    }

    public UserService getUserService() {
        return (UserService) HM_TEST_INJECTOR.get("UserService");
    }

    public PersistenceEntryManager getPersistenceEntryManager() {
        return (PersistenceEntryManager) HM_TEST_INJECTOR.get("PersistenceEntryManager");
    }
}
