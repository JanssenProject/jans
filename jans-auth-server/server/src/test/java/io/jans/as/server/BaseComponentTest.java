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
import io.jans.service.cdi.util.CdiUtil;

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
        return CdiUtil.bean(InumGenerator.class);
    }

    public static StaticConfiguration getStaticConfiguration() {
        return CdiUtil.bean(StaticConfiguration.class);
    }

    public ClientService getClientService() {
        return CdiUtil.bean(ClientService.class);
    }

    public InumService getInumService() {
        return CdiUtil.bean(InumService.class);
    }

    public CleanerTimer getCleanerTimer() {
        return CdiUtil.bean(CleanerTimer.class);
    }

    public CacheService getCacheService() {
        return CdiUtil.bean(CacheService.class);
    }

    public UmaRptService getUmaRptService() {
        return CdiUtil.bean(UmaRptService.class);
    }

    public UmaResourceService getUmaResourceService() {
        return CdiUtil.bean(UmaResourceService.class);
    }

    public UmaPermissionService getUmaPermissionService() {
        return CdiUtil.bean(UmaPermissionService.class);
    }

    public UmaPctService getUmaPctService() {
        return CdiUtil.bean(UmaPctService.class);
    }

    public AuthorizationGrantList getAuthorizationGrantList() {
        return CdiUtil.bean(AuthorizationGrantList.class);
    }

    public GrantService getGrantService() {
        return CdiUtil.bean(GrantService.class);
    }

    public EncryptionService getEncryptionService() {
        return CdiUtil.bean(EncryptionService.class);
    }

    public ConfigurationFactory getConfigurationFactory() {
        return CdiUtil.bean(ConfigurationFactory.class);
    }

    public AbstractCryptoProvider getAbstractCryptoProvider() {
        return CdiUtil.bean(AbstractCryptoProvider.class);
    }

    public SessionIdService getSessionIdService() {
        return CdiUtil.bean(SessionIdService.class);
    }

    public UserService getUserService() {
        return CdiUtil.bean(UserService.class);
    }

    public PersistenceEntryManager getPersistenceEntryManager() {
        return CdiUtil.bean(PersistenceEntryManager.class);
    }
}
