/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.server.idgen.ws.rs.InumGenerator;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.uma.service.UmaPctService;
import io.jans.as.server.uma.service.UmaPermissionService;
import io.jans.as.server.uma.service.UmaResourceService;
import io.jans.as.server.uma.service.UmaRptService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.CacheService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.HashMap;

@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class TestInjectionService {

    public static HashMap<String, Object> HM_TEST_INJECTOR = new HashMap<>();

    @Inject
    private InumGenerator inumGenerator;
    @Inject
    private StaticConfiguration staticConfiguration;
    @Inject
    private ClientService clientService;
    @Inject
    private InumService inumService;
    @Inject
    private CleanerTimer cleanerTimer;
    @Inject
    private CacheService cacheService;
    @Inject
    private UmaRptService umaRptService;
    @Inject
    private UmaResourceService umaResourceService;
    @Inject
    private UmaPermissionService umaPermissionService;
    @Inject
    private UmaPctService umaPctService;
    @Inject
    private AuthorizationGrantList authorizationGrantList;
    @Inject
    private GrantService grantService;
    @Inject
    private EncryptionService encryptionService;
    @Inject
    private ConfigurationFactory configurationFactory;
    @Inject
    private AbstractCryptoProvider cryptoProvider;
    @Inject
    private SessionIdService sessionIdService;
    @Inject
    private UserService userService;
    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    public void initInjection() {
        HM_TEST_INJECTOR.put("InumGenerator", inumGenerator);
        HM_TEST_INJECTOR.put("StaticConfiguration", staticConfiguration);
        HM_TEST_INJECTOR.put("ClientService", clientService);
        HM_TEST_INJECTOR.put("InumService", inumService);
        HM_TEST_INJECTOR.put("CleanerTimer", cleanerTimer);
        HM_TEST_INJECTOR.put("CacheService", cacheService);
        HM_TEST_INJECTOR.put("UmaRptService", umaRptService);
        HM_TEST_INJECTOR.put("UmaResourceService", umaResourceService);
        HM_TEST_INJECTOR.put("UmaPermissionService", umaPermissionService);
        HM_TEST_INJECTOR.put("UmaPctService", umaPctService);
        HM_TEST_INJECTOR.put("AuthorizationGrantList", authorizationGrantList);
        HM_TEST_INJECTOR.put("GrantService", grantService);
        HM_TEST_INJECTOR.put("EncryptionService", encryptionService);
        HM_TEST_INJECTOR.put("ConfigurationFactory", configurationFactory);
        HM_TEST_INJECTOR.put("AbstractCryptoProvider", cryptoProvider);
        HM_TEST_INJECTOR.put("SessionIdService", sessionIdService);
        HM_TEST_INJECTOR.put("UserService", userService);
        HM_TEST_INJECTOR.put("PersistenceEntryManager", persistenceEntryManager);
    }

}