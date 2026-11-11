package io.jans.ca.plugin.adminui.test.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.ca.plugin.adminui.utils.security.PolicyToScopeMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertEquals;

public class PolicyToScopeMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private Logger log;

    @InjectMocks
    private PolicyToScopeMapper policyToScopeMapper;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testProcessZipFileMapsAdminUiRbacPolicyToScope() throws Exception {
        String policy = "permit(principal, action, resource) when { "
                + "principal.getTag(\"jansAdminUIRole\").contains(\"admin\") && "
                + "resource in GluuFlexAdminUIResources::Features::\"users\" && "
                + "action == GluuFlexAdminUI::Action::\"READ\" };";
        Path archive = createPolicyArchive(policy);

        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            Map<String, Set<String>> result = policyToScopeMapper.processZipFile(zipFile, resourceScopes());
            assertEquals(result, Map.of("admin", Set.of("users.read")));
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    private Path createPolicyArchive(String policy) throws IOException {
        Path archive = Files.createTempFile("admin-ui-rbac-policy-", ".zip");
        try (ZipOutputStream zipOutput = new ZipOutputStream(Files.newOutputStream(archive))) {
            zipOutput.putNextEntry(new ZipEntry("policies/admin-ui.cedar"));
            zipOutput.write(policy.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zipOutput.closeEntry();
        }
        return archive;
    }

    private JsonNode resourceScopes() throws IOException {
        return OBJECT_MAPPER.readTree("[{\"resource\":\"users\",\"accessType\":\"READ\","
                + "\"scopes\":[\"users.read\"]}]");
    }
}
