package io.jans.ca.plugin.adminui.service.adminui;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.ca.plugin.adminui.model.adminui.AdminUIPolicyStore;
import io.jans.ca.plugin.adminui.model.adminui.AdminUIResourceScopesMapping;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.ca.plugin.adminui.utils.security.PolicyToScopeMapper;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.document.store.model.Document;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Service responsible for managing Admin UI security related operations such as
 * retrieving, uploading, synchronizing and updating the Cedarling policy store.
 *
 * <p>This service interacts with:
 * <ul>
 *     <li>Local policy store files</li>
 *     <li>Remote policy store endpoints</li>
 *     <li>Jans persistence layer</li>
 *     <li>Admin UI role and permission configuration</li>
 * </ul>
 *
 * <p>It also supports synchronization between Cedar policy definitions and
 * Admin UI role-to-scope mappings.</p>
 */

@Singleton
public class AdminUISecurityService {

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    AdminUIService adminUIService;

    @Inject
    PolicyToScopeMapper policyToScopeMapper;

    private static final String TRUSTED_ISSUER_FILE = "trusted-issuers/GluuFlexAdminUI.json";

    /**
     * Retrieves the current policy store from the configured local file system path.
     *
     * <p>The policy store path is resolved using the following precedence:</p>
     * <ol>
     *    <li>Configured value in {@link AUIConfiguration#getAuiCedarlingDefaultPolicyStorePath()}</li>
     *     <li>{@link AppConstants#DEFAULT_POLICY_STORE_FILE_PATH}</li>
     * </ol>
     *
     * <p>If the file exists, the method returns the binary content of the policy store
     * (typically a .cjar archive). If the file does not exist, a 404 response is returned.</p>
     *
     * @return {@link GenericResponse} containing the policy store file as a byte array
     * @throws ApplicationException if an unexpected error occurs while retrieving the file
     */
    public GenericResponse getPolicyStore() throws ApplicationException {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            // Resolve path for local policy store file
            String policyStorePath = Optional.ofNullable(auiConfiguration.getAuiCedarlingDefaultPolicyStorePath())
                    .filter(path -> !Strings.isNullOrEmpty(path))
                    .orElse(AppConstants.DEFAULT_POLICY_STORE_FILE_PATH);
            Path path = Paths.get(policyStorePath);
            if (!Files.exists(path)) {
                throw new ApplicationException(Response.Status.NOT_FOUND.getStatusCode(), "Policy store not found.");
            }
            byte[] zipBytes = Files.readAllBytes(path);

            return CommonUtils.createResponseWithByteArray(true, 200, "Policy store fetched successfully.", zipBytes);
        } catch (ApplicationException e) {
            throw e; // Re-throw ApplicationException as is
        } catch (Exception e) {
            log.error(ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription());
        }
    }

    /**
     * Uploads and overwrites the existing policy store file on the server.
     *
     * <p>This method performs the following operations:
     * <ul>
     *     <li>Validates the incoming request and file metadata</li>
     *     <li>Ensures the uploaded file has a valid <code>.cjar</code> extension</li>
     *     <li>Validates the input stream of the uploaded policy store</li>
     *     <li>Resolves the configured policy store path</li>
     *     <li>Validates the domain inside the existing policy store against the server host</li>
     *     <li>Creates a backup of the existing policy store file (if present)</li>
     *     <li>Uploads and replaces the policy store with the new file</li>
     * </ul>
     *
     * @param adminUIPolicyStore the {@link AdminUIPolicyStore} containing the policy store file
     *                           and its associated metadata
     * @return a {@link GenericResponse} indicating success or failure of the upload operation
     * @throws ApplicationException if:
     *                              <ul>
     *                                  <li>The request or document is null</li>
     *                                  <li>The file name is missing or does not have a <code>.cjar</code> extension</li>
     *                                  <li>The input stream is invalid or empty</li>
     *                                  <li>The policy store domain does not match the configured server host</li>
     *                                  <li>Any error occurs during validation, backup, or file upload</li>
     *                              </ul>
     */

    public GenericResponse uploadPolicyStore(AdminUIPolicyStore adminUIPolicyStore) throws ApplicationException {
        try {
            validateRequest(adminUIPolicyStore);

            Document cjarDocument = adminUIPolicyStore.getDocument();
            log.info("Uploading policy-store : {}", cjarDocument.getFileName());

            InputStream cjarStream = adminUIPolicyStore.getPolicyStore();
            //copy into a variable so that it can be used later
            byte[] cjarBytes = cjarStream.readAllBytes();

            InputStream cjarStreamForValidation = new ByteArrayInputStream(cjarBytes);
            InputStream cjarStreamForUpload = new ByteArrayInputStream(cjarBytes);

            validateInputStream(cjarStreamForValidation);

            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            String policyStorePath = resolvePolicyStorePath(auiConfiguration);

            // Validate domain inside policy store
            validatePolicyStoreDomain(cjarStreamForValidation, auiConfiguration.getAuiWebServerHost());

            Path path = Paths.get(policyStorePath);

            // Backup existing file
            backupExistingPolicyStore(path);
            try {
                // Upload new file
                Files.copy(cjarStreamForUpload, path, StandardCopyOption.REPLACE_EXISTING);

                log.info("Uploaded policy-store : {}", cjarDocument.getFileName());
            } catch(Exception e) {
                restoreFromBackup(path);
                throw new ApplicationException(
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        e.getMessage());
            }

            return CommonUtils.createGenericResponse(true, 200,
                    "Policy store overwritten successfully.");

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.POLICY_STORE_UPLOAD_ERROR.getDescription(), e);
            throw new ApplicationException(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    e.getMessage()
            );
        }
    }

    private void validateRequest(AdminUIPolicyStore adminUIPolicyStore) throws ApplicationException {
        if (adminUIPolicyStore == null) {
            log.error(ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription());
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription()
            );
        }

        Document document = adminUIPolicyStore.getDocument();
        if (document == null || document.getFileName() == null) {
            log.error("Document details not provided.");
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription()
            );
        }

        if (!document.getFileName().endsWith(".cjar")) {
            log.error(ErrorResponse.UNSUPPORTED_POLICY_STORE_EXTENSION.getDescription());
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.UNSUPPORTED_POLICY_STORE_EXTENSION.getDescription()
            );
        }
    }

    private void validateInputStream(InputStream inputStream) throws ApplicationException {
        try {
            if (inputStream == null || inputStream.available() <= 0) {
                log.error(ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription());
                throw new ApplicationException(
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription()
                );
            }
        } catch (IOException e) {
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Error in reading policy-store. Invalid input stream"
            );
        }
    }

    private String resolvePolicyStorePath(AUIConfiguration config) {
        return Optional.ofNullable(config.getAuiCedarlingDefaultPolicyStorePath())
                .filter(path -> !Strings.isNullOrEmpty(path))
                .orElse(AppConstants.DEFAULT_POLICY_STORE_FILE_PATH);
    }

    private void validatePolicyStoreDomain(InputStream cjarStream, String webServerHost)
            throws ApplicationException, URISyntaxException {

        URI uri = new URI(webServerHost);
        if (!isHostnameMatching(cjarStream, uri.getHost())) {
            log.error(ErrorResponse.POLICY_STORE_DOMAIN_NOT_MATCHING.getDescription());
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.POLICY_STORE_DOMAIN_NOT_MATCHING.getDescription()
            );
        }
    }

    private void backupExistingPolicyStore(Path path) throws IOException {
        if (Files.exists(path)) {
            Path backupPath = Paths.get(path.toString() + ".bak");
            Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void restoreFromBackup(Path path) {
        try {
            Path backupPath = Paths.get(path.toString() + ".bak");
            if (Files.exists(backupPath)) {
                Files.move(backupPath, path, StandardCopyOption.REPLACE_EXISTING);
                log.info("Restored policy store from backup");
            }
        } catch (IOException e) {
            log.error("Failed to restore policy store from backup", e);
        }
    }

    /**
     * Synchronizes Admin UI role-to-scope mappings using the currently configured Cedar policy store.
     *
     * <p>The synchronization process includes:</p>
     * <ol>
     *     <li>Retrieving resource-to-scope mappings from persistence</li>
     *     <li>Parsing the Cedar policy store archive (.cjar)</li>
     *     <li>Deriving principal-to-scope mappings from policies</li>
     *     <li>Generating Admin UI roles from the principals</li>
     *     <li>Generating role-permission mappings</li>
     *     <li>Removing duplicate permissions</li>
     *     <li>Updating Admin UI roles and permissions</li>
     * </ol>
     *
     * <p>This ensures that Admin UI access control remains consistent with
     * Cedar authorization policies.</p>
     *
     * @return {@link GenericResponse} indicating success or failure of the synchronization process
     * @throws ApplicationException if synchronization fails due to validation or system errors
     */

    public GenericResponse syncRoleScopeMapping() throws ApplicationException {
        try {
            // Retrieve all Admin UI resource-scope mappings from persistence
            final Filter filter = Filter.createPresenceFilter(AppConstants.ADMIN_UI_RESOURCE);
            List<AdminUIResourceScopesMapping> adminUIResourceScopesMappings =
                    entryManager.findEntries(AppConstants.ADMIN_UI_RESOURCE_SCOPES_MAPPING_DN,
                            AdminUIResourceScopesMapping.class, filter);

            // Convert resource-scope mappings to JSON safely
            JsonNode resourceScopesJson = CommonUtils.toJsonNode(adminUIResourceScopesMappings);

            // Validate inputs
            if (resourceScopesJson == null) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(),
                        "Invalid input data for synchronization");
            }
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            String policyStorePath = Optional.ofNullable(auiConfiguration.getAuiCedarlingDefaultPolicyStorePath())
                    .filter(path -> !Strings.isNullOrEmpty(path))
                    .orElse(AppConstants.DEFAULT_POLICY_STORE_FILE_PATH);
            Map<String, Set<String>> principalsToScopesMap;
            try (ZipFile zipFile = new ZipFile(policyStorePath)) {
                principalsToScopesMap = policyToScopeMapper.processZipFile(zipFile, resourceScopesJson);
            } catch (Exception e) {
                log.error(ErrorResponse.ERROR_IN_POLICY_STORE.getDescription(), e);
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(),
                        ErrorResponse.ERROR_IN_POLICY_STORE.getDescription());
            }

            // Validate mapping results
            if (principalsToScopesMap.isEmpty()) {
                log.warn("No role-to-scope mappings found during synchronization");
            }

            List<AdminRole> roles = createAdminRoles(principalsToScopesMap.keySet());
            List<RolePermissionMapping> rolePermissionMappings = createRolePermissionMappings(principalsToScopesMap);

            // Remove duplicate permissions efficiently
            List<RolePermissionMapping> updatedMappings = removeDuplicatePermissions(rolePermissionMappings);

            // Update services
            adminUIService.resetRoles(roles);
            adminUIService.resetPermissionsToRole(updatedMappings);

            return CommonUtils.createGenericResponse(true, 200,
                    "Sync of role-to-scope mappings from the policy store completed successfully.");

        } catch (ApplicationException e) {
            throw e; // Re-throw ApplicationException as is
        } catch (Exception e) {
            log.error(ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription());
        }
    }

    /**
     * Extracted method for creating AdminRole objects
     */
    private List<AdminRole> createAdminRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(roleName -> {
                    AdminRole role = new AdminRole();
                    role.setRole(roleName);
                    role.setDescription("Role created after parsing policy-store for " + roleName);
                    role.setDeletable(true);
                    return role;
                })
                .toList();
    }

    /**
     * Extracted method for creating RolePermissionMapping objects
     */
    private List<RolePermissionMapping> createRolePermissionMappings(Map<String, Set<String>> principalsToScopesMap) {
        return principalsToScopesMap.entrySet().stream()
                .map(entry -> {
                    RolePermissionMapping rpm = new RolePermissionMapping();
                    rpm.setRole(entry.getKey());
                    rpm.setPermissions(new ArrayList<>(entry.getValue()));
                    return rpm;
                })
                .toList();
    }

    /**
     * Create a new list of role-to-permission mappings where duplicate permissions for each role are removed while preserving their original order.
     *
     * @param rolePermissionMappings the list of role-to-permission mappings to process; each mapping's permissions may contain duplicates
     * @return a new list of RolePermissionMapping with duplicates removed from each mapping's permissions (iteration order preserved)
     */
    private List<RolePermissionMapping> removeDuplicatePermissions(List<RolePermissionMapping> rolePermissionMappings) {
        return rolePermissionMappings.stream()
                .map(entry -> {
                    RolePermissionMapping rpm = new RolePermissionMapping();
                    rpm.setRole(entry.getRole());
                    // Use LinkedHashSet to maintain order while removing duplicates
                    rpm.setPermissions(new ArrayList<>(new LinkedHashSet<>(entry.getPermissions())));
                    return rpm;
                })
                .toList();
    }

    /**
     * Validates that the trusted issuer's `configuration_endpoint` host inside the provided JSON stream matches the expected domain.
     *
     * @param zipInputStream an InputStream containing the JSON content of the trusted issuer (e.g., the `trusted-issuers/GluuFlexAdminUI.json` entry)
     * @param expectedDomain the domain expected to match the `configuration_endpoint` host
     * @return `true` if the `configuration_endpoint` host equals `expectedDomain` (case-insensitive), `false` otherwise
     * @throws ApplicationException if the input cannot be read or parsed, or if the `configuration_endpoint` value is not a valid URI
     */
    private boolean isHostnameMatching(InputStream zipInputStream, String expectedDomain)
            throws ApplicationException {

        if (zipInputStream == null || expectedDomain == null || expectedDomain.isBlank()) {
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Invalid input: zipInputStream or expectedDomain is null/empty"
            );
        }

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.isDirectory() || !TRUSTED_ISSUER_FILE.equals(entry.getName())) {
                    zis.closeEntry();
                    continue;
                }

                // Read content safely
                String content = readEntryContent(zis);

                JSONObject json = new JSONObject(content);

                if (!json.has("configuration_endpoint")) {
                    throw new ApplicationException(
                            Response.Status.BAD_REQUEST.getStatusCode(),
                            "Missing 'configuration_endpoint' in JSON"
                    );
                }

                String endpoint = json.getString("configuration_endpoint");
                URI uri = new URI(endpoint);
                String domain = uri.getHost();

                log.info("Domain of OpenID Provider: {}, Domain of trusted issuer: {}",
                        expectedDomain, domain);

                return domain != null && domain.equalsIgnoreCase(expectedDomain);
            }

            // File not found in zip
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Trusted issuer file not found in ZIP"
            );

        } catch (ApplicationException e) {
            throw e; // rethrow
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_POLICY_STORE.getDescription(), e);
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Error processing ZIP: " + e.getMessage()
            );
        }
    }

    private String readEntryContent(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int len;

        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }

        return baos.toString(StandardCharsets.UTF_8);
    }
}
