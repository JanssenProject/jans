package io.jans.ca.plugin.adminui.test.services;

import io.jans.ca.plugin.adminui.model.adminui.AdminUIPolicyStore;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.adminui.AdminUISecurityService;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link AdminUISecurityService} covering the methods invoked
 * from {@code AdminUISecurityResource}: searchPolicyStores, uploadPolicyStore,
 * editPolicyStore, deletePolicyStore, syncRoleScopeMapping and getRecordMaxCount.
 */
@Listeners(MockitoTestNGListener.class)
public class AdminUISecurityServiceTest {

    private static final String INUM = "1234-5678";

    @Mock
    private Logger log;
    @Mock
    private PersistenceEntryManager entryManager;
    @Mock
    private AUIConfigurationService auiConfigurationService;
    @Mock
    private ConfigurationFactory configurationFactory;
    @Mock
    private io.jans.ca.plugin.adminui.service.adminui.AdminUIService adminUIService;
    @Mock
    private io.jans.ca.plugin.adminui.utils.security.PolicyToScopeMapper policyToScopeMapper;

    @InjectMocks
    private AdminUISecurityService adminUISecurityService;

    private static final String WEB_SERVER_HOST = "https://test-jans.io";
    private static final String POLICY_STORE_FIXTURE = "/json/adminui/test-policy-store.cjar";

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Loads the packaged {@code .cjar} test fixture and returns it base64-encoded,
     * as stored in the {@code policyStore} document field.
     */
    private String loadPolicyStoreBase64() throws java.io.IOException {
        try (java.io.InputStream is = getClass().getResourceAsStream(POLICY_STORE_FIXTURE)) {
            assertNotNull(is, "Missing test fixture: " + POLICY_STORE_FIXTURE);
            return java.util.Base64.getEncoder().encodeToString(is.readAllBytes());
        }
    }

    // ---------------------------------------------------------------------
    // searchPolicyStores
    // ---------------------------------------------------------------------

    @Test
    public void testSearchPolicyStores_success() throws ApplicationException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setSortBy(AppConstants.INUM);
        searchRequest.setSortOrder(SortOrder.ASCENDING.getValue());
        searchRequest.setStartIndex(0);
        searchRequest.setCount(10);
        searchRequest.setMaxCount(100);

        PagedResult<AdminUIPolicyStore> expected = new PagedResult<>();
        when(entryManager.findPagedEntries(anyString(), eq(AdminUIPolicyStore.class), any(),
                isNull(), anyString(), any(SortOrder.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        PagedResult<AdminUIPolicyStore> result = adminUISecurityService.searchPolicyStores(searchRequest);

        assertSame(result, expected);
    }

    @Test
    public void testSearchPolicyStores_persistenceFailureWrapped() {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setSortBy(AppConstants.INUM);
        searchRequest.setSortOrder(SortOrder.ASCENDING.getValue());
        lenient().when(entryManager.findPagedEntries(anyString(), any(), any(), any(), any(),
                any(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("db down"));

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.searchPolicyStores(searchRequest));
        assertEquals(ex.getErrorCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    // ---------------------------------------------------------------------
    // uploadPolicyStore
    // ---------------------------------------------------------------------

    @Test
    public void testUploadPolicyStore_nullRequestThrowsBadRequest() {
        // validateRequest fails -> wrapped as INTERNAL_SERVER_ERROR by uploadPolicyStore
        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.uploadPolicyStore(null));
        assertEquals(ex.getErrorCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        verify(entryManager, never()).persist(any());
    }

    @Test
    public void testUploadPolicyStore_missingPolicyStoreDocument() {
        AdminUIPolicyStore store = new AdminUIPolicyStore();
        store.setDisplayname("test");
        // policyStore document is null -> validation fails

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.uploadPolicyStore(store));
        assertEquals(ex.getErrorCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        verify(entryManager, never()).persist(any());
    }

    @Test
    public void testUploadPolicyStore_success() throws Exception {
        io.jans.configapi.core.model.adminui.AUIConfiguration auiConfiguration =
                mock(io.jans.configapi.core.model.adminui.AUIConfiguration.class);
        when(auiConfigurationService.getAUIConfiguration()).thenReturn(auiConfiguration);
        when(auiConfiguration.getAuiWebServerHost()).thenReturn(WEB_SERVER_HOST);

        AdminUIPolicyStore store = new AdminUIPolicyStore();
        store.setDisplayname("test-store");
        store.setPolicyStore(loadPolicyStoreBase64());

        GenericResponse response = adminUISecurityService.uploadPolicyStore(store);

        assertTrue(response.isSuccess());
        assertEquals(response.getResponseCode(), 200);

        ArgumentCaptor<AdminUIPolicyStore> captor = ArgumentCaptor.forClass(AdminUIPolicyStore.class);
        verify(entryManager).persist(captor.capture());
        AdminUIPolicyStore persisted = captor.getValue();
        assertNotNull(persisted.getInum());
        assertEquals(persisted.getDn(), adminUISecurityService.getDnForPolicyStore(persisted.getInum()));
        assertEquals(persisted.getJansStatus(), AppConstants.STATUS_INACTIVE);
        assertNotNull(persisted.getCreationDate());
    }

    @Test
    public void testUploadPolicyStore_domainMismatchRejected() throws Exception {
        io.jans.configapi.core.model.adminui.AUIConfiguration auiConfiguration =
                mock(io.jans.configapi.core.model.adminui.AUIConfiguration.class);
        when(auiConfigurationService.getAUIConfiguration()).thenReturn(auiConfiguration);
        when(auiConfiguration.getAuiWebServerHost()).thenReturn("https://someone-else.example.com");

        AdminUIPolicyStore store = new AdminUIPolicyStore();
        store.setDisplayname("test-store");
        store.setPolicyStore(loadPolicyStoreBase64());

        // domain does not match the fixture's trusted-issuer host -> wrapped as 500 by uploadPolicyStore
        expectThrows(ApplicationException.class, () -> adminUISecurityService.uploadPolicyStore(store));
        verify(entryManager, never()).persist(any());
    }

    // ---------------------------------------------------------------------
    // editPolicyStore
    // ---------------------------------------------------------------------

    @Test
    public void testEditPolicyStore_nullRequestReturnsBadRequest() {
        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.editPolicyStore(INUM, null));
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testEditPolicyStore_blankInumReturnsBadRequest() {
        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.editPolicyStore("", new AdminUIPolicyStore()));
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testEditPolicyStore_notFound() {
        when(entryManager.find(eq(AdminUIPolicyStore.class), anyString())).thenReturn(null);

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.editPolicyStore(INUM, new AdminUIPolicyStore()));
        assertEquals(ex.getErrorCode(), Response.Status.NOT_FOUND.getStatusCode());
        verify(entryManager, never()).merge(any());
    }

    @Test
    public void testEditPolicyStore_success() throws ApplicationException {
        AdminUIPolicyStore existing = new AdminUIPolicyStore();
        existing.setInum(INUM);
        existing.setDisplayname("old");
        when(entryManager.find(eq(AdminUIPolicyStore.class), anyString())).thenReturn(existing);

        AdminUIPolicyStore update = new AdminUIPolicyStore();
        update.setDisplayname("new-name");
        update.setDescription("new-desc");
        update.setJansStatus(AppConstants.STATUS_ACTIVE);

        GenericResponse response = adminUISecurityService.editPolicyStore(INUM, update);

        assertTrue(response.isSuccess());
        assertEquals(response.getResponseCode(), 200);

        ArgumentCaptor<AdminUIPolicyStore> captor = ArgumentCaptor.forClass(AdminUIPolicyStore.class);
        verify(entryManager).merge(captor.capture());
        AdminUIPolicyStore merged = captor.getValue();
        assertEquals(merged.getDisplayname(), "new-name");
        assertEquals(merged.getDescription(), "new-desc");
        assertEquals(merged.getJansStatus(), AppConstants.STATUS_ACTIVE);
        // read-only field preserved
        assertEquals(merged.getInum(), INUM);
    }

    @Test
    public void testEditPolicyStore_activatingDemotesOtherActiveStore() throws ApplicationException {
        AdminUIPolicyStore existing = new AdminUIPolicyStore();
        existing.setInum(INUM);
        existing.setJansStatus(AppConstants.STATUS_INACTIVE);
        when(entryManager.find(eq(AdminUIPolicyStore.class), anyString())).thenReturn(existing);

        // Another store is currently active and must be demoted.
        AdminUIPolicyStore otherActive = new AdminUIPolicyStore();
        otherActive.setInum("other-inum");
        otherActive.setJansStatus(AppConstants.STATUS_ACTIVE);
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class)))
                .thenReturn(java.util.List.of(otherActive));

        AdminUIPolicyStore update = new AdminUIPolicyStore();
        update.setJansStatus(AppConstants.STATUS_ACTIVE);

        GenericResponse response = adminUISecurityService.editPolicyStore(INUM, update);

        assertTrue(response.isSuccess());

        ArgumentCaptor<AdminUIPolicyStore> captor = ArgumentCaptor.forClass(AdminUIPolicyStore.class);
        verify(entryManager, times(2)).merge(captor.capture());
        java.util.List<AdminUIPolicyStore> merged = captor.getAllValues();
        // the previously-active store was demoted...
        AdminUIPolicyStore demoted = merged.stream()
                .filter(s -> "other-inum".equals(s.getInum())).findFirst().orElseThrow();
        assertEquals(demoted.getJansStatus(), AppConstants.STATUS_INACTIVE);
        // ...and the edited store is now active
        AdminUIPolicyStore activated = merged.stream()
                .filter(s -> INUM.equals(s.getInum())).findFirst().orElseThrow();
        assertEquals(activated.getJansStatus(), AppConstants.STATUS_ACTIVE);
    }

    @Test
    public void testEditPolicyStore_activatingSelfDoesNotDemoteItself() throws ApplicationException {
        AdminUIPolicyStore existing = new AdminUIPolicyStore();
        existing.setInum(INUM);
        existing.setJansStatus(AppConstants.STATUS_ACTIVE);
        when(entryManager.find(eq(AdminUIPolicyStore.class), anyString())).thenReturn(existing);

        // The only active store is the one being edited; it must not be demoted.
        AdminUIPolicyStore self = new AdminUIPolicyStore();
        self.setInum(INUM);
        self.setJansStatus(AppConstants.STATUS_ACTIVE);
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class)))
                .thenReturn(java.util.List.of(self));

        AdminUIPolicyStore update = new AdminUIPolicyStore();
        update.setJansStatus(AppConstants.STATUS_ACTIVE);

        GenericResponse response = adminUISecurityService.editPolicyStore(INUM, update);

        assertTrue(response.isSuccess());
        // only the edited store is merged (once); nothing gets demoted
        ArgumentCaptor<AdminUIPolicyStore> captor = ArgumentCaptor.forClass(AdminUIPolicyStore.class);
        verify(entryManager, times(1)).merge(captor.capture());
        assertEquals(captor.getValue().getJansStatus(), AppConstants.STATUS_ACTIVE);
    }

    // ---------------------------------------------------------------------
    // deletePolicyStore
    // ---------------------------------------------------------------------

    @Test
    public void testDeletePolicyStore_blankInumReturnsBadRequest() {
        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.deletePolicyStore(""));
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        verify(entryManager, never()).remove(any());
    }

    @Test
    public void testDeletePolicyStore_notFound() {
        when(entryManager.find(eq(AdminUIPolicyStore.class), anyString())).thenReturn(null);

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.deletePolicyStore(INUM));
        assertEquals(ex.getErrorCode(), Response.Status.NOT_FOUND.getStatusCode());
        verify(entryManager, never()).remove(any());
    }

    @Test
    public void testDeletePolicyStore_success() throws ApplicationException {
        AdminUIPolicyStore existing = new AdminUIPolicyStore();
        existing.setInum(INUM);
        when(entryManager.find(eq(AdminUIPolicyStore.class), anyString())).thenReturn(existing);

        GenericResponse response = adminUISecurityService.deletePolicyStore(INUM);

        assertTrue(response.isSuccess());
        assertEquals(response.getResponseCode(), 200);
        verify(entryManager).remove(existing);
    }

    // ---------------------------------------------------------------------
    // syncRoleScopeMapping
    // ---------------------------------------------------------------------

    @Test
    public void testSyncRoleScopeMapping_noActivePolicyStore() {
        // resource-scope mappings lookup returns empty list (non-null -> JSON node is created)
        when(entryManager.findEntries(anyString(), any(), any(Filter.class)))
                .thenReturn(java.util.Collections.emptyList());

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.syncRoleScopeMapping());
        assertEquals(ex.getErrorCode(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(ex.getMessage(), ErrorResponse.NO_ACTIVE_POLICY_STORE_FOUND.getDescription());
    }

    @Test
    public void testSyncRoleScopeMapping_success() throws Exception {
        // resource-scope mappings lookup (presence filter)
        when(entryManager.findEntries(anyString(),
                eq(io.jans.ca.plugin.adminui.model.adminui.AdminUIResourceScopesMapping.class),
                any(Filter.class)))
                .thenReturn(java.util.Collections.emptyList());

        // active policy-store lookup (equality filter) -> one active store with the fixture document
        AdminUIPolicyStore activeStore = new AdminUIPolicyStore();
        activeStore.setInum(INUM);
        activeStore.setJansStatus(AppConstants.STATUS_ACTIVE);
        activeStore.setPolicyStore(loadPolicyStoreBase64());
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class)))
                .thenReturn(java.util.List.of(activeStore));

        // policy-store parsing is delegated to the mapper (mocked): return a role with a duplicate scope
        java.util.Map<String, java.util.Set<String>> mapped = new java.util.HashMap<>();
        mapped.put("adminRole", new java.util.LinkedHashSet<>(java.util.List.of("scope-read", "scope-write")));
        when(policyToScopeMapper.processZipFile(any(), any())).thenReturn(mapped);

        GenericResponse response = adminUISecurityService.syncRoleScopeMapping();

        assertTrue(response.isSuccess());
        assertEquals(response.getResponseCode(), 200);
        verify(policyToScopeMapper).processZipFile(any(), any());
        verify(adminUIService).resetRoles(any());
        verify(adminUIService).resetPermissionsToRole(any());
    }

    // ---------------------------------------------------------------------
    // getRecordMaxCount
    // ---------------------------------------------------------------------

    @Test
    public void testGetRecordMaxCount_usesConfiguredValue() {
        ApiAppConfiguration apiAppConfiguration = mock(ApiAppConfiguration.class);
        when(configurationFactory.getApiAppConfiguration()).thenReturn(apiAppConfiguration);
        when(apiAppConfiguration.getMaxCount()).thenReturn(250);

        assertEquals(adminUISecurityService.getRecordMaxCount(), 250);
    }

    @Test
    public void testGetRecordMaxCount_fallsBackToDefaultWhenNotPositive() {
        ApiAppConfiguration apiAppConfiguration = mock(ApiAppConfiguration.class);
        when(configurationFactory.getApiAppConfiguration()).thenReturn(apiAppConfiguration);
        when(apiAppConfiguration.getMaxCount()).thenReturn(0);

        assertEquals(adminUISecurityService.getRecordMaxCount(),
                io.jans.configapi.util.ApiConstants.DEFAULT_MAX_COUNT);
    }
}
