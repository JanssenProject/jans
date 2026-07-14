package io.jans.ca.plugin.adminui.service.adminui;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.jans.as.common.util.AttributeConstants;
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
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.tika.Tika;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static io.jans.ca.plugin.adminui.utils.AppConstants.POLICY_STORE_DN;

/**
 * Service responsible for managing Admin UI security related operations such as
 * searching, uploading, editing, deleting and synchronizing the Cedarling policy store.
 *
 * <p>This service interacts with:
 * <ul>
 *     <li>The Jans persistence layer, where policy stores are persisted (the Cedar
 *         archive is held as a base64-encoded document)</li>
 *     <li>Admin UI role and permission configuration</li>
 * </ul>
 *
 * <p>It also synchronizes Admin UI role-to-scope mappings from the active policy
 * store held in persistence.</p>
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
    ConfigurationFactory configurationFactory;

    @Inject
    AdminUIService adminUIService;

    @Inject
    PolicyToScopeMapper policyToScopeMapper;

    Tika tika = new Tika();

    private static final long MAX_ENTRY_SIZE = 3_000_000; // 3 MB

    private static final String TRUSTED_ISSUER_FILE = "trusted-issuers/GluuFlexAdminUI.json";

    public PagedResult<AdminUIPolicyStore> searchPolicyStores(SearchRequest searchRequest) throws ApplicationException {
        try {
            Filter searchFilter = null;
            List<Filter> filters = new ArrayList<>();
            if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

                for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                    String[] targetArray = new String[]{assertionValue};
                    Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                            targetArray, null);
                    Filter policyStoreIdFilter = Filter.createSubstringFilter(AppConstants.INUM, null, targetArray, null);
                    Filter statusFilter = Filter.createSubstringFilter(AppConstants.STATUS, null, targetArray, null);
                    filters.add(Filter.createORFilter(policyStoreIdFilter, statusFilter, displayNameFilter));
                }
                searchFilter = Filter.createORFilter(filters);
            }
            log.debug("AdminUIPolicyStore searchFilter:{}", searchFilter);
            return entryManager.findPagedEntries(POLICY_STORE_DN, AdminUIPolicyStore.class, searchFilter, null,
                    searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                    searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

        } catch (Exception e) {
            log.error(ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription());
        }
    }

    public GenericResponse uploadPolicyStore(AdminUIPolicyStore adminUIPolicyStore) throws ApplicationException {
        try {
            validateRequest(adminUIPolicyStore);

            log.info("Uploading policy-store : {}", adminUIPolicyStore.getDisplayname());
            String inum = UUID.randomUUID().toString();

            adminUIPolicyStore.setInum(inum);
            adminUIPolicyStore.setDn(getDnForPolicyStore(inum));
            Date now = new Date();
            adminUIPolicyStore.setCreationDate(now);
            adminUIPolicyStore.setJansLastUpd(now);
            adminUIPolicyStore.setJansStatus(AppConstants.STATUS_INACTIVE);
            entryManager.persist(adminUIPolicyStore);

            return CommonUtils.createGenericResponse(true, 200,
                    "Policy store saved successfully.");

        } catch (Exception e) {
            log.error(ErrorResponse.POLICY_STORE_UPLOAD_ERROR.getDescription(), e);
            throw new ApplicationException(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    e.getMessage()
            );
        }
    }

    public GenericResponse editPolicyStore(String inum, AdminUIPolicyStore adminUIPolicyStore) throws ApplicationException {
        try {
            if (adminUIPolicyStore == null || Strings.isNullOrEmpty(inum)) {
                throw new ApplicationException(
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription()
                );
            }

            AdminUIPolicyStore existing = getPolicyStoreByInum(inum);
            if (existing == null) {
                throw new ApplicationException(
                        Response.Status.NOT_FOUND.getStatusCode(),
                        ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription()
                );
            }

            log.info("edit policy-store : {}", inum);
            // Apply only the editable fields; read-only fields (inum, dn,
            // creationDate, jansUsrDN, policyStore) are preserved from persistence.
            existing.setDisplayname(adminUIPolicyStore.getDisplayname());
            existing.setDescription(adminUIPolicyStore.getDescription());
            if (!Strings.isNullOrEmpty(adminUIPolicyStore.getJansStatus())) {
                // Only one policy-store may be active at a time. If this store is being
                // activated, demote any other currently-active store to inactive first.
                if (AppConstants.STATUS_ACTIVE.equalsIgnoreCase(adminUIPolicyStore.getJansStatus())) {
                    deactivateOtherActivePolicyStores(inum);
                }
                existing.setJansStatus(adminUIPolicyStore.getJansStatus());
            }
            existing.setJansLastUpd(new Date());

            entryManager.merge(existing);

            return CommonUtils.createGenericResponse(true, 200,
                    "Policy store updated successfully.");

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.POLICY_STORE_UPDATE_ERROR.getDescription(), e);
            throw new ApplicationException(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    e.getMessage()
            );
        }
    }

    /**
     * Loads a policy store by its inum.
     *
     * @param inum the policy store inum
     * @return the matching {@link AdminUIPolicyStore}, or {@code null} if none exists
     */
    private AdminUIPolicyStore getPolicyStoreByInum(String inum) {
        try {
            return entryManager.find(AdminUIPolicyStore.class, getDnForPolicyStore(inum));
        } catch (Exception e) {
            log.debug("Policy store not found for inum: {}", inum, e);
            return null;
        }
    }

    public GenericResponse deletePolicyStore(String inum) throws ApplicationException {
        if (Strings.isNullOrEmpty(inum)) {
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription()
            );
        }

        AdminUIPolicyStore existing = getPolicyStoreByInum(inum);
        if (existing == null) {
            throw new ApplicationException(
                    Response.Status.NOT_FOUND.getStatusCode(),
                    ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription()
            );
        }
        log.info("delete policy-store : {}", inum);
        entryManager.remove(existing);

        return CommonUtils.createGenericResponse(true, 200,
                "Policy store deleted successfully.");
    }

    /**
     * Demotes every active policy-store other than {@code inumToKeep} to inactive,
     * enforcing the "only one active policy-store" invariant.
     *
     * @param inumToKeep the inum of the store that is about to become active and must be left untouched
     */
    private void deactivateOtherActivePolicyStores(String inumToKeep) {
        Filter activeFilter = Filter.createEqualityFilter(AppConstants.STATUS, AppConstants.STATUS_ACTIVE);
        List<AdminUIPolicyStore> activeStores =
                entryManager.findEntries(POLICY_STORE_DN, AdminUIPolicyStore.class, activeFilter);

        if (CollectionUtils.isEmpty(activeStores)) {
            return;
        }
        for (AdminUIPolicyStore activeStore : activeStores) {
            if (inumToKeep.equals(activeStore.getInum())) {
                continue;
            }
            log.info("Deactivating previously active policy-store : {}", activeStore.getInum());
            activeStore.setJansStatus(AppConstants.STATUS_INACTIVE);
            activeStore.setJansLastUpd(new Date());
            entryManager.merge(activeStore);
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

        String base64PolicyStore = adminUIPolicyStore.getPolicyStore();
        if (Strings.isNullOrEmpty(base64PolicyStore)) {
            log.error("PolicyStore is null.");
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription()
            );
        }

        byte[] zipBytes;
        try {
            zipBytes = Base64.getDecoder().decode(base64PolicyStore);
        } catch (IllegalArgumentException e) {
            log.error(ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription(), e);
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.BAD_REQUEST_IN_POLICY_STORE_UPLOAD.getDescription()
            );
        }

        String mimeType = tika.detect(zipBytes);
        if (Strings.isNullOrEmpty(mimeType) || !mimeType.equalsIgnoreCase("application/zip")) {
            log.error(ErrorResponse.UNSUPPORTED_POLICY_STORE_EXTENSION.getDescription());
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.UNSUPPORTED_POLICY_STORE_EXTENSION.getDescription()
            );
        }

        try (InputStream cjarStream = new ByteArrayInputStream(zipBytes)) {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            validatePolicyStoreDomain(cjarStream, auiConfiguration.getAuiWebServerHost());
        } catch (ApplicationException e) {
            throw e;
        } catch (URISyntaxException | IOException e) {
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    e.getMessage()
            );
        }
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

    /**
     * Synchronizes Admin UI role-to-scope mappings using the active Cedar policy store stored in the database.
     *
     * <p>The synchronization process includes:</p>
     * <ol>
     *     <li>Retrieving resource-to-scope mappings from persistence</li>
     *     <li>Fetching the active policy store from the database and parsing its Cedar archive (.cjar)</li>
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
            // Fetch the active policy-store from the database (its document is stored as base64).
            AdminUIPolicyStore activePolicyStore = getActivePolicyStore();
            if (activePolicyStore == null || Strings.isNullOrEmpty(activePolicyStore.getPolicyStore())) {
                log.error(ErrorResponse.NO_ACTIVE_POLICY_STORE_FOUND.getDescription());
                throw new ApplicationException(Response.Status.NOT_FOUND.getStatusCode(),
                        ErrorResponse.NO_ACTIVE_POLICY_STORE_FOUND.getDescription());
            }

            Map<String, Set<String>> principalsToScopesMap = mapPrincipalsToScopes(activePolicyStore, resourceScopesJson);

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
     * Retrieves the single active policy-store from persistence.
     *
     * @return the active {@link AdminUIPolicyStore}, or {@code null} if none is active
     */
    private AdminUIPolicyStore getActivePolicyStore() {
        Filter activeFilter = Filter.createEqualityFilter(AppConstants.STATUS, AppConstants.STATUS_ACTIVE);
        List<AdminUIPolicyStore> activeStores =
                entryManager.findEntries(POLICY_STORE_DN, AdminUIPolicyStore.class, activeFilter);

        if (CollectionUtils.isEmpty(activeStores)) {
            return null;
        }
        if (activeStores.size() > 1) {
            log.warn("Found {} active policy-stores; using the first one.", activeStores.size());
        }
        return activeStores.get(0);
    }

    /**
     * Decodes the base64 policy-store document into a temporary .cjar archive and derives
     * principal-to-scope mappings from it. The temporary file is always removed afterwards.
     *
     * @param policyStore      the active policy-store whose document should be parsed
     * @param resourceScopesJson the resource-to-scope mappings used during parsing
     * @return principal-to-scope mappings derived from the policy-store
     * @throws ApplicationException if the document cannot be decoded or parsed
     */
    private Map<String, Set<String>> mapPrincipalsToScopes(AdminUIPolicyStore policyStore, JsonNode resourceScopesJson)
            throws ApplicationException {
        Path tempFile = null;
        try {
            byte[] zipBytes = Base64.getDecoder().decode(policyStore.getPolicyStore());
            tempFile = Files.createTempFile("adminui-policy-store-", ".cjar");
            Files.write(tempFile, zipBytes);

            try (ZipFile zipFile = new ZipFile(tempFile.toFile())) {
                return policyToScopeMapper.processZipFile(zipFile, resourceScopesJson);
            }
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_POLICY_STORE.getDescription(), e);
            throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ErrorResponse.ERROR_IN_POLICY_STORE.getDescription());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary policy-store file: {}", tempFile, e);
                }
            }
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
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
    }

    public String getDnForPolicyStore(String inum) {
        return String.format("inum=%s,%s", inum, POLICY_STORE_DN);
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
                String entryName = entry.getName();
                // Basic validation
                if (entry.isDirectory() || entryName == null ||
                        entryName.contains("..") || entryName.startsWith("/")) {
                    zis.closeEntry();
                    continue;
                }
                if (!TRUSTED_ISSUER_FILE.equals(entryName)) {
                    zis.closeEntry();
                    continue;
                }
                // Read safely with limit
                String content = readEntryContentSafely(zis);

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
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Trusted issuer file not found in ZIP"
            );
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_POLICY_STORE.getDescription(), e);
            throw new ApplicationException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Error processing ZIP: " + e.getMessage()
            );
        }
    }

    private String readEntryContentSafely(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        long totalRead = 0;

        while ((len = is.read(buffer)) != -1) {
            totalRead += len;
            if (totalRead > MAX_ENTRY_SIZE) {
                throw new IOException("ZIP entry too large (possible ZIP bomb greater than 3 MB)");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    public int getRecordMaxCount() {
        log.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }
}
