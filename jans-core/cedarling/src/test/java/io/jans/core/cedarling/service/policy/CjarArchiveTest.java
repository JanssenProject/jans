package io.jans.core.cedarling.service.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class CjarArchiveTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static byte[] zip(Map<String, String> entries) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(out)) {
			for (Map.Entry<String, String> entry : entries.entrySet()) {
				zos.putNextEntry(new ZipEntry(entry.getKey()));
				zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				zos.closeEntry();
			}
		}
		return out.toByteArray();
	}

	@Test
	void readsPolicyStoreFromRootMetadata() throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put("policies/allow.cedar", "permit(principal, action, resource);");
		entries.put("metadata.json", "{\"cedar_version\":\"4.4.0\",\"policy_store\":{\"id\":\"abc123def456\",\"name\":\"store\",\"version\":\"1.0.0\"}}");

		JsonNode policyStore = CjarArchive.readPolicyStoreMetadata(zip(entries), 0, objectMapper);

		assertEquals("abc123def456", CjarArchive.text(policyStore, "id"));
		assertEquals("store", CjarArchive.text(policyStore, "name"));
		assertEquals("1.0.0", CjarArchive.text(policyStore, "version"));
	}

	@Test
	void returnsNullIdWhenMetadataHasNoId() throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put("metadata.json", "{\"cedar_version\":\"4.4.0\",\"policy_store\":{}}");

		assertNull(CjarArchive.text(CjarArchive.readPolicyStoreMetadata(zip(entries), 0, objectMapper), "id"));
	}

	@Test
	void returnsNullForLegacyZip() throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put("store.json", "{\"cedar_version\":\"v4.0.0\",\"policy_stores\":{}}");
		entries.put("nested/metadata.json", "{}");

		assertNull(CjarArchive.readPolicyStoreMetadata(zip(entries), 0, objectMapper));
	}

	@Test
	void rejectsTooManyEntries() throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put("a.cedar", "");
		entries.put("b.cedar", "");
		entries.put("metadata.json", "{}");
		byte[] archive = zip(entries);

		assertThrows(IllegalStateException.class, () -> CjarArchive.readPolicyStoreMetadata(archive, 2, objectMapper));
	}

	@Test
	void rejectsTooManyEntriesAfterMetadata() throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put("metadata.json", "{\"policy_store\":{\"id\":\"abc\"}}");
		entries.put("a.cedar", "");
		entries.put("b.cedar", "");
		byte[] archive = zip(entries);

		assertThrows(IllegalStateException.class, () -> CjarArchive.readPolicyStoreMetadata(archive, 2, objectMapper));
	}

	@Test
	void rejectsMalformedMetadata() throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put("metadata.json", "{not json");
		byte[] archive = zip(entries);

		assertThrows(IllegalStateException.class, () -> CjarArchive.readPolicyStoreMetadata(archive, 0, objectMapper));
	}

	@Test
	void rejectsEmptyMetadata() throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put("metadata.json", "");
		byte[] archive = zip(entries);

		assertThrows(IllegalStateException.class, () -> CjarArchive.readPolicyStoreMetadata(archive, 0, objectMapper));
	}

	@Test
	void rejectsArchiveOverUncompressedLimit() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(out)) {
			zos.putNextEntry(new ZipEntry("metadata.json"));
			zos.write("{\"policy_store\":{\"id\":\"abc\"}}".getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("policies/big.cedar"));
			byte[] chunk = new byte[1024 * 1024];
			for (long written = 0; written <= CjarArchive.MAX_TOTAL_UNCOMPRESSED_BYTES; written += chunk.length) {
				zos.write(chunk);
			}
			zos.closeEntry();
		}
		byte[] archive = out.toByteArray();

		assertThrows(IllegalStateException.class, () -> CjarArchive.readPolicyStoreMetadata(archive, 0, objectMapper));
	}

	@Test
	void computesSha256BundleHash() {
		assertEquals("sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
				CjarArchive.bundleHash("abc".getBytes(StandardCharsets.UTF_8)));
	}

}
