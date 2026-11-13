package io.jans.ca.plugin.adminui.test.services;

import io.jans.ca.plugin.adminui.model.adminui.AdminUIPolicyStore;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.adminui.AdminUISecurityService;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.ca.plugin.adminui.utils.security.PolicyToScopeMapper;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private PolicyToScopeMapper policyToScopeMapper;
    @InjectMocks
    private PolicyToScopeMapper actualPolicyToScopeMapper;

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
    private String loadPolicyStoreBase64() throws IOException {
        return Base64.getEncoder().encodeToString(loadPolicyStoreBytes());
    }

    private byte[] loadPolicyStoreBytes() throws IOException {
        try (java.io.InputStream is = getClass().getResourceAsStream(POLICY_STORE_FIXTURE)) {
            assertNotNull(is, "Missing test fixture: " + POLICY_STORE_FIXTURE);
            return is.readAllBytes();
        }
    }

    @Test
    public void testCreateSecureTempFile_usesRestrictedTempDirectory() throws Exception {
        Method createSecureTempFile = AdminUISecurityService.class.getDeclaredMethod("createSecureTempFile", String.class, String.class);
        createSecureTempFile.setAccessible(true);

        Path tempFile = (Path) createSecureTempFile.invoke(null, "adminui-policy-store-", ".cjar");
        assertNotNull(tempFile);
        assertTrue(Files.exists(tempFile));

        Path parentDir = tempFile.getParent();
        assertNotNull(parentDir);
        assertTrue(parentDir.getFileName().toString().startsWith("adminui-policy-store-"));

        if (Files.getFileAttributeView(parentDir, PosixFileAttributeView.class) != null) {
            java.util.Set<PosixFilePermission> perms = Files.getPosixFilePermissions(parentDir);
            assertFalse(perms.contains(PosixFilePermission.OTHERS_READ));
            assertFalse(perms.contains(PosixFilePermission.OTHERS_WRITE));
            assertFalse(perms.contains(PosixFilePermission.OTHERS_EXECUTE));
        }

        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(parentDir);
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
        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.uploadPolicyStore(null));
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        verify(entryManager, never()).persist(any());
    }

    @Test
    public void testUploadPolicyStore_missingPolicyStoreDocument() {
        AdminUIPolicyStore store = new AdminUIPolicyStore();
        store.setDisplayname("test");

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.uploadPolicyStore(store));
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        verify(entryManager, never()).persist(any());
    }

    @Test
    public void testUploadPolicyStore_oversizedBase64RejectedBeforeDecoding() {
        AdminUIPolicyStore store = new AdminUIPolicyStore();
        store.setPolicyStore("A".repeat(3_000_001));

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.uploadPolicyStore(store));

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(ex.getMessage(), ErrorResponse.POLICY_STORE_UPLOAD_TOO_LARGE.getDescription());
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
    public void testValidateTrustedIssuerDomainsRejectsRedirectedEntryWithDifferentContentOnAllowedHost()
            throws Exception {
        byte[] zipBytes = createPolicyStoreWithSecondaryIssuer();
        redirectCentralEntry(zipBytes,
                "trusted-issuers/GluuFlexAdminUI.json",
                "trusted-issuers/Secondary.json");

        ApplicationException ex = validateArchiveExpectingFailure(zipBytes);

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(ex.getMessage(), "Inconsistent policy-store archive entries");
    }

    @Test
    public void testUploadPolicyStore_invalidCedarlingArchiveReturnsBadRequest() throws Exception {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(archiveBytes)) {
            zipOutput.putNextEntry(new ZipEntry("invalid.txt"));
            zipOutput.write("not a Cedarling policy store".getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();
        }
        io.jans.configapi.core.model.adminui.AUIConfiguration auiConfiguration =
                mock(io.jans.configapi.core.model.adminui.AUIConfiguration.class);
        when(auiConfigurationService.getAUIConfiguration()).thenReturn(auiConfiguration);
        when(auiConfiguration.getAuiWebServerHost()).thenReturn(WEB_SERVER_HOST);

        AdminUIPolicyStore store = new AdminUIPolicyStore();
        store.setDisplayname("invalid-store");
        store.setPolicyStore(Base64.getEncoder().encodeToString(archiveBytes.toByteArray()));

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.uploadPolicyStore(store));
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        verify(entryManager, never()).persist(any());
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

    @Test
    public void testValidateTrustedIssuerDomainsRejectsMismatchedSecondaryIssuer() throws Exception {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(archiveBytes)) {
            zipOutput.putNextEntry(new ZipEntry("trusted-issuers/GluuFlexAdminUI.json"));
            zipOutput.write("{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\"}"
                    .getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();

            zipOutput.putNextEntry(new ZipEntry("trusted-issuers/Secondary.json"));
            zipOutput.write("{\"configuration_endpoint\":\"https://issuer.example.invalid/.well-known/openid-configuration\"}"
                    .getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();
        }

        Method validateTrustedIssuerDomains = AdminUISecurityService.class.getDeclaredMethod(
                "validateTrustedIssuerDomains", byte[].class, String.class);
        validateTrustedIssuerDomains.setAccessible(true);
        java.lang.reflect.InvocationTargetException invocationException = expectThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> validateTrustedIssuerDomains.invoke(
                        adminUISecurityService, archiveBytes.toByteArray(), WEB_SERVER_HOST));

        ApplicationException ex = (ApplicationException) invocationException.getCause();
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(ex.getMessage(), ErrorResponse.POLICY_STORE_DOMAIN_NOT_MATCHING.getDescription());
    }

    @DataProvider(name = "invalidIssuerEndpoints")
    public Object[][] invalidIssuerEndpoints() {
        return new Object[][]{
                {"userinfo-prefixed host", "https://test-jans.io@issuer.example.invalid/.well-known/openid-configuration"},
                {"host hidden in fragment", "https://issuer.example.invalid/.well-known/openid-configuration#https://test-jans.io"},
                {"relative path", "/.well-known/openid-configuration"}
        };
    }

    @Test(dataProvider = "invalidIssuerEndpoints")
    public void testValidateTrustedIssuerDomainsRejectsEndpointWithoutExpectedHost(
            String caseName, String endpoint) throws Exception {
        ApplicationException ex = validateArchiveExpectingFailure(createZipArchive(
                "trusted-issuers/GluuFlexAdminUI.json",
                "{\"configuration_endpoint\":\"" + endpoint + "\"}"));

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode(), caseName);
        assertEquals(ex.getMessage(), ErrorResponse.POLICY_STORE_DOMAIN_NOT_MATCHING.getDescription(), caseName);
    }

    @Test
    public void testValidateTrustedIssuerDomainsRejectsMismatchedRootIssuersFile() throws Exception {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(archiveBytes)) {
            zipOutput.putNextEntry(new ZipEntry("trusted-issuers/GluuFlexAdminUI.json"));
            zipOutput.write("{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\"}"
                    .getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();

            zipOutput.putNextEntry(new ZipEntry("trusted-issuers.json"));
            zipOutput.write(("{\"Acme::Access_token\":{\"type\":\"jwt\",\"issuer\":\"Acme\",\"configuration_endpoints\":"
                    + "{\"jwks\":\"https://issuer.example.invalid/keys\",\"iss\":\"https://issuer.example.invalid\"}}}")
                    .getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();
        }

        Method validateTrustedIssuerDomains = AdminUISecurityService.class.getDeclaredMethod(
                "validateTrustedIssuerDomains", byte[].class, String.class);
        validateTrustedIssuerDomains.setAccessible(true);
        java.lang.reflect.InvocationTargetException invocationException = expectThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> validateTrustedIssuerDomains.invoke(
                        adminUISecurityService, archiveBytes.toByteArray(), WEB_SERVER_HOST));

        ApplicationException ex = (ApplicationException) invocationException.getCause();
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(ex.getMessage(), ErrorResponse.POLICY_STORE_DOMAIN_NOT_MATCHING.getDescription());
    }

    @Test
    public void testValidateTrustedIssuerDomainsRejectsParentTraversalEntryName() throws Exception {
        ApplicationException ex = validateArchiveExpectingFailure(createZipArchive(
                "../trusted-issuers/GluuFlexAdminUI.json",
                "{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\"}"));

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(ex.getMessage(), "Invalid policy-store file path");
    }

    @Test
    public void testValidateTrustedIssuerDomainsRejectsAbsoluteEntryName() throws Exception {
        ApplicationException ex = validateArchiveExpectingFailure(createZipArchive(
                "/trusted-issuers/GluuFlexAdminUI.json",
                "{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\"}"));

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(ex.getMessage(), "Invalid policy-store file path");
    }

    @Test
    public void testValidateTrustedIssuerDomainsDetectsUppercaseJsonExtension() throws Exception {
        ApplicationException ex = validateArchiveExpectingFailure(createZipArchive(
                "trusted-issuers/Foo.JSON",
                "{\"configuration_endpoint\":\"https://issuer.example.invalid/.well-known/openid-configuration\"}"));

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(ex.getMessage(), ErrorResponse.POLICY_STORE_DOMAIN_NOT_MATCHING.getDescription());
    }

    @Test
    public void testValidateTrustedIssuerDomainsRejectsMissingAdminUiIssuerFile() throws Exception {
        ApplicationException ex = validateArchiveExpectingFailure(createZipArchive(
                "trusted-issuers/Other.json",
                "{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\"}"));

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertTrue(ex.getMessage().contains("Trusted issuer file not found in ZIP"));
    }

    @Test
    public void testValidateTrustedIssuerDomainsRejectsDuplicateEntryNames() throws Exception {
        String firstName = "trusted-issuers/GluuFlexAdminUI.json";
        String secondName = "trusted-issuers/GluuFlexAdminUj.json";
        byte[] zipBytes = createZipArchive(firstName,
                "{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\"}",
                secondName,
                "{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\"}");
        replaceBytes(zipBytes, secondName.getBytes(StandardCharsets.UTF_8),
                firstName.getBytes(StandardCharsets.UTF_8));

        ApplicationException ex = validateArchiveExpectingFailure(zipBytes);

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(ex.getMessage(), "Inconsistent policy-store archive entries");
    }

    @Test
    public void testValidateTrustedIssuerDomainsCountsUtf8BytesForArchiveLimit() throws Exception {
        String issuerContent = "{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\","
                + "\"padding\":\"" + "\u00e9".repeat(1_400_000) + "\"}";
        byte[] issuerContentBytes = issuerContent.getBytes(StandardCharsets.UTF_8);
        assertTrue(issuerContentBytes.length < 3_000_000, "Each issuer entry must remain below the per-entry limit");

        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(archiveBytes)) {
            for (int i = 0; i < 11; i++) {
                String entryName = i == 0
                        ? "trusted-issuers/GluuFlexAdminUI.json"
                        : "trusted-issuers/Secondary" + i + ".json";
                zipOutput.putNextEntry(new ZipEntry(entryName));
                zipOutput.write(issuerContentBytes);
                zipOutput.closeEntry();
            }
        }

        Method validateTrustedIssuerDomains = AdminUISecurityService.class.getDeclaredMethod(
                "validateTrustedIssuerDomains", byte[].class, String.class);
        validateTrustedIssuerDomains.setAccessible(true);
        java.lang.reflect.InvocationTargetException invocationException = expectThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> validateTrustedIssuerDomains.invoke(
                        adminUISecurityService, archiveBytes.toByteArray(), WEB_SERVER_HOST));

        ApplicationException ex = (ApplicationException) invocationException.getCause();
        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertTrue(ex.getMessage().contains("ZIP archive too large when decompressed"));
    }

    @Test
    public void testValidateTrustedIssuerDomainsEnforcesCentralDirectoryIssuerEntryLimit() throws Exception {
        String issuerContent = "{\"configuration_endpoint\":\"https://test-jans.io/.well-known/openid-configuration\","
                + "\"padding\":\"" + "x".repeat(3_000_001) + "\"}";

        ApplicationException ex = validateArchiveExpectingFailure(createZipArchive(
                "trusted-issuers/GluuFlexAdminUI.json", issuerContent));

        assertEquals(ex.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertTrue(ex.getMessage().contains("ZIP entry too large"));
    }

    private ApplicationException validateArchiveExpectingFailure(byte[] zipBytes) throws Exception {
        Method validateTrustedIssuerDomains = AdminUISecurityService.class.getDeclaredMethod(
                "validateTrustedIssuerDomains", byte[].class, String.class);
        validateTrustedIssuerDomains.setAccessible(true);
        java.lang.reflect.InvocationTargetException invocationException = expectThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> validateTrustedIssuerDomains.invoke(
                        adminUISecurityService, zipBytes, WEB_SERVER_HOST));
        return (ApplicationException) invocationException.getCause();
    }

    private byte[] createZipArchive(String entryName, String content) throws IOException {
        return createZipArchive(new String[]{entryName}, new String[]{content});
    }

    private byte[] createZipArchive(
            String firstEntryName,
            String firstContent,
            String secondEntryName,
            String secondContent) throws IOException {
        return createZipArchive(
                new String[]{firstEntryName, secondEntryName},
                new String[]{firstContent, secondContent});
    }

    private byte[] createZipArchive(String[] entryNames, String[] contents) throws IOException {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(archiveBytes)) {
            for (int i = 0; i < entryNames.length; i++) {
                zipOutput.putNextEntry(new ZipEntry(entryNames[i]));
                zipOutput.write(contents[i].getBytes(StandardCharsets.UTF_8));
                zipOutput.closeEntry();
            }
        }
        return archiveBytes.toByteArray();
    }

    private byte[] createPolicyStoreWithSecondaryIssuer() throws IOException {
        byte[] fixture = loadPolicyStoreBytes();
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (java.util.zip.ZipInputStream zipInput = new java.util.zip.ZipInputStream(
                     new java.io.ByteArrayInputStream(fixture));
             ZipOutputStream zipOutput = new ZipOutputStream(archiveBytes)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                zipOutput.putNextEntry(new ZipEntry(entry.getName()));
                zipOutput.write(zipInput.readAllBytes());
                zipOutput.closeEntry();
            }
            zipOutput.putNextEntry(new ZipEntry("trusted-issuers/Secondary.json"));
            zipOutput.write(("{\"name\":\"Secondary issuer\",\"description\":\"Test trusted issuer\","
                    + "\"openid_configuration_endpoint\":\"https://test-jans.io/.well-known/secondary-configuration\","
                    + "\"configuration_endpoint\":\"https://test-jans.io/.well-known/secondary-configuration\"}")
                    .getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();
        }
        return archiveBytes.toByteArray();
    }

    private void redirectCentralEntry(byte[] zipBytes, String entryName, String targetEntryName) {
        int entryCentralOffset = findCentralEntryOffset(zipBytes, entryName);
        int targetCentralOffset = findCentralEntryOffset(zipBytes, targetEntryName);
        int targetLocalOffset = readLittleEndianInt(zipBytes, targetCentralOffset + 42);
        writeLittleEndianInt(zipBytes, entryCentralOffset + 42, targetLocalOffset);
        System.arraycopy(zipBytes, targetCentralOffset + 16, zipBytes, entryCentralOffset + 16, 12);
    }

    private int findLocalEntryOffset(byte[] zipBytes, String expectedName) {
        byte[] expectedNameBytes = expectedName.getBytes(StandardCharsets.UTF_8);
        for (int offset = 0; offset <= zipBytes.length - 30; offset++) {
            if (readLittleEndianInt(zipBytes, offset) != 0x04034b50) {
                continue;
            }
            int nameLength = readLittleEndianShort(zipBytes, offset + 26);
            if (nameLength == expectedNameBytes.length
                    && matches(zipBytes, offset + 30, expectedNameBytes)) {
                return offset;
            }
        }
        throw new AssertionError("Local ZIP entry not found: " + expectedName);
    }

    private int findCentralEntryOffset(byte[] zipBytes, String expectedName) {
        byte[] expectedNameBytes = expectedName.getBytes(StandardCharsets.UTF_8);
        for (int offset = 0; offset <= zipBytes.length - 46; offset++) {
            if (readLittleEndianInt(zipBytes, offset) != 0x02014b50) {
                continue;
            }
            int nameLength = readLittleEndianShort(zipBytes, offset + 28);
            if (nameLength == expectedNameBytes.length
                    && matches(zipBytes, offset + 46, expectedNameBytes)) {
                return offset;
            }
        }
        throw new AssertionError("Central ZIP entry not found: " + expectedName);
    }

    private void replaceBytes(byte[] target, byte[] from, byte[] to) {
        assertEquals(from.length, to.length);
        int replacements = 0;
        for (int i = 0; i <= target.length - from.length; i++) {
            if (matches(target, i, from)) {
                System.arraycopy(to, 0, target, i, to.length);
                replacements++;
                i += from.length - 1;
            }
        }
        assertEquals(replacements, 2, "Expected to rename both local and central entries");
    }

    private boolean matches(byte[] target, int offset, byte[] expected) {
        if (offset < 0 || offset > target.length - expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (target[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 3] & 0xff) << 24);
    }

    private int readLittleEndianShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    private void writeLittleEndianInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
        bytes[offset + 2] = (byte) (value >>> 16);
        bytes[offset + 3] = (byte) (value >>> 24);
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
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class), eq(1)))
                .thenReturn(null);

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
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class), eq(1)))
                .thenReturn(java.util.List.of(existing));

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
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class), eq(1)))
                .thenReturn(java.util.List.of(existing));

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
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class), eq(1)))
                .thenReturn(java.util.List.of(existing));

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

    @Test
    public void testEditPolicyStore_activationFailureRestoresDemotedStore() {
        AdminUIPolicyStore existing = new AdminUIPolicyStore();
        existing.setInum(INUM);
        existing.setJansStatus(AppConstants.STATUS_INACTIVE);
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class), eq(1)))
                .thenReturn(java.util.List.of(existing));

        AdminUIPolicyStore otherActive = new AdminUIPolicyStore();
        otherActive.setInum("other-inum");
        otherActive.setJansStatus(AppConstants.STATUS_ACTIVE);
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class)))
                .thenReturn(java.util.List.of(otherActive));

        doAnswer(invocation -> {
            AdminUIPolicyStore store = invocation.getArgument(0);
            if (INUM.equals(store.getInum()) && AppConstants.STATUS_ACTIVE.equals(store.getJansStatus())) {
                throw new IllegalStateException("activation failed");
            }
            return null;
        }).when(entryManager).merge(any(AdminUIPolicyStore.class));

        AdminUIPolicyStore update = new AdminUIPolicyStore();
        update.setJansStatus(AppConstants.STATUS_ACTIVE);

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.editPolicyStore(INUM, update));

        assertEquals(ex.getErrorCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(otherActive.getJansStatus(), AppConstants.STATUS_ACTIVE);
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
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class), eq(1)))
                .thenReturn(java.util.List.of());

        ApplicationException ex = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.deletePolicyStore(INUM));
        assertEquals(ex.getErrorCode(), Response.Status.NOT_FOUND.getStatusCode());
        verify(entryManager, never()).remove(any());
    }

    @Test
    public void testDeletePolicyStore_success() throws ApplicationException {
        AdminUIPolicyStore existing = new AdminUIPolicyStore();
        existing.setInum(INUM);
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class), eq(1)))
                .thenReturn(java.util.List.of(existing));

        GenericResponse response = adminUISecurityService.deletePolicyStore(INUM);

        assertTrue(response.isSuccess());
        assertEquals(response.getResponseCode(), 200);
        verify(entryManager).remove(existing);
    }

    @Test
    public void testDeletePolicyStore_logsOnlyInum() throws ApplicationException {
        AdminUIPolicyStore existing = new AdminUIPolicyStore();
        existing.setInum(INUM);
        existing.setDisplayname("bad\r\nINFO forged");
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class), eq(1)))
                .thenReturn(java.util.List.of(existing));

        adminUISecurityService.deletePolicyStore(INUM);

        verify(log).info(eq("delete policy-store, INUM : {}"), eq(INUM));
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
        verify(adminUIService).resetPermissionsToRoleFromPolicySync(any(),eq(java.util.Collections.emptySet()));
    }

    @DataProvider(name = "unsupportedSyncPolicies")
    public Object[][] unsupportedSyncPolicies() {
        return new Object[][] {
                { "forbid(principal, action, resource) when { "
                        + "principal.getTag(\"jansAdminUIRole\").contains(\"admin\") && "
                        + "resource in GluuFlexAdminUIResources::Features::\"users\" && "
                        + "action == GluuFlexAdminUI::Action::\"READ\" };" },
                { "permit(principal, action, resource) unless { "
                        + "principal.getTag(\"jansAdminUIRole\").contains(\"admin\") && "
                        + "resource in GluuFlexAdminUIResources::Features::\"users\" && "
                        + "action == GluuFlexAdminUI::Action::\"READ\" };" }
        };
    }

    @Test(dataProvider = "unsupportedSyncPolicies")
    public void testSyncRoleScopeMappingRejectsUnsupportedPolicyWithoutResettingRolesOrPermissions(
            String policy) throws Exception {
        when(entryManager.findEntries(anyString(),
                eq(io.jans.ca.plugin.adminui.model.adminui.AdminUIResourceScopesMapping.class),
                any(Filter.class)))
                .thenReturn(java.util.Collections.emptyList());

        AdminUIPolicyStore activeStore = new AdminUIPolicyStore();
        activeStore.setInum(INUM);
        activeStore.setJansStatus(AppConstants.STATUS_ACTIVE);
        activeStore.setPolicyStore(createPolicyStoreBase64(policy));
        when(entryManager.findEntries(anyString(), eq(AdminUIPolicyStore.class), any(Filter.class)))
                .thenReturn(java.util.List.of(activeStore));
        when(policyToScopeMapper.processZipFile(any(), any())).thenAnswer(invocation ->
                actualPolicyToScopeMapper.processZipFile(invocation.getArgument(0), invocation.getArgument(1)));

        ApplicationException exception = expectThrows(ApplicationException.class,
                () -> adminUISecurityService.syncRoleScopeMapping());

        assertEquals(exception.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        verify(adminUIService, never()).resetRoles(any());
        verify(adminUIService, never()).resetPermissionsToRoleFromPolicySync(any(), any());
    }

    private String createPolicyStoreBase64(String policy) throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(archive)) {
            zipOutput.putNextEntry(new ZipEntry("policies/admin-ui.cedar"));
            zipOutput.write(policy.getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();
        }
        return Base64.getEncoder().encodeToString(archive.toByteArray());
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