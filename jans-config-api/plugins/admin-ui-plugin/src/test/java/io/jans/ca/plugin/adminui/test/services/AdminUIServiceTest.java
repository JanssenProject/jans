package io.jans.ca.plugin.adminui.test.services;

import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.AdminPermission;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.DynamicConfig;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.adminui.AdminUIService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.orm.PersistenceEntryManager;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class AdminUIServiceTest {

    @Mock
    private Logger log;

    @Mock
    private PersistenceEntryManager entryManager;

    @InjectMocks
    private AdminUIService adminUIService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DataProvider(name = "nonCatalogPermissionUpdates")
    public Object[][] nonCatalogPermissionUpdates() {
        return new Object[][] {
                { List.of("legacy.permission") },
                { List.of("legacy.permission", "catalog.permission") }
        };
    }

    @Test(dataProvider = "nonCatalogPermissionUpdates")
    public void testMapPermissionsToRoleRejectsNonCatalogPermissionAndPreservesExistingMapping(
            List<String> requestedPermissions) {
        AdminRole role = new AdminRole();
        role.setRole("admin");
        AdminPermission catalogPermission = new AdminPermission();
        catalogPermission.setPermission("catalog.permission");
        RolePermissionMapping existingMapping = new RolePermissionMapping();
        existingMapping.setRole("admin");
        existingMapping.setPermissions(new ArrayList<>(List.of("legacy.permission")));
        List<RolePermissionMapping> existingMappings = new ArrayList<>(List.of(existingMapping));

        DynamicConfig dynamicConfig = new DynamicConfig();
        dynamicConfig.setRoles(List.of(role));
        dynamicConfig.setPermissions(List.of(catalogPermission));
        dynamicConfig.setRolePermissionMapping(existingMappings);
        AdminConf adminConf = new AdminConf();
        adminConf.setDynamic(dynamicConfig);
        when(entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN)).thenReturn(adminConf);

        RolePermissionMapping request = new RolePermissionMapping();
        request.setRole("admin");
        request.setPermissions(requestedPermissions);

        ApplicationException exception = expectThrows(ApplicationException.class,
                () -> adminUIService.mapPermissionsToRole(request));

        assertEquals(exception.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(dynamicConfig.getRolePermissionMapping(), existingMappings);
        assertEquals(existingMapping.getPermissions(), List.of("legacy.permission"));
        verify(entryManager, never()).merge(adminConf);
    }

    @Test
    public void testResetPermissionsToRoleFromPolicySyncAllowsResourceScopeOutsideCatalog() throws Exception {
        AdminRole role = new AdminRole();
        role.setRole("admin");
        DynamicConfig dynamicConfig = new DynamicConfig();
        dynamicConfig.setRoles(List.of(role));
        dynamicConfig.setPermissions(List.of());
        AdminConf adminConf = new AdminConf();
        adminConf.setDynamic(dynamicConfig);
        when(entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN)).thenReturn(adminConf);

        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setRole("admin");
        mapping.setPermissions(List.of("resource.scope"));

        adminUIService.resetPermissionsToRoleFromPolicySync(List.of(mapping), java.util.Set.of("resource.scope"));

        assertEquals(dynamicConfig.getRolePermissionMapping(), List.of(mapping));
        verify(entryManager).merge(adminConf);
    }
}
