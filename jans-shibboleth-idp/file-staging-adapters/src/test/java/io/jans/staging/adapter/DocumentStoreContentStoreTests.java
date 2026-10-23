package io.jans.staging.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.staging.ContentSource;
import io.jans.staging.ContentType;
import io.jans.staging.Handle;
import io.jans.staging.error.ContentUnreadable;
import io.jans.staging.port.StoredContent;

import java.io.IOException;
import java.security.MessageDigest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DocumentStoreContentStore — streaming store, rename move, remove")
public class DocumentStoreContentStoreTests {

    private static final byte[] BYTES = "<EntityDescriptor/>".getBytes();
    private static final Handle STAGING = Handle.of("/var/lib/jans/staging/tok-1.xml");
    private static final Handle DURABLE = Handle.of("/opt/shibboleth-idp/metadata/tok-1.xml");

    private InMemoryDocumentStore documentStore;
    private DocumentStoreContentStore contentStore;

    @BeforeEach
    void setUp() {

        documentStore = new InMemoryDocumentStore();
        contentStore = new DocumentStoreContentStore(documentStore);
    }

    private static String sha256Hex(byte[] bytes) throws Exception {

        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {

            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    @DisplayName("GIVEN a content source WHEN stored THEN it streams to the store and reports size, hash and content type")
    public void storeStreamsAndReportsMetadata() throws Exception {

        StoredContent stored = contentStore.store(STAGING, ContentType.of("application/samlmetadata+xml"),
            ContentSource.ofBytes(BYTES)).getValue();

        assertThat(stored.size()).isEqualTo(BYTES.length);
        assertThat(stored.hash().getValue()).isEqualTo(sha256Hex(BYTES));
        assertThat(documentStore.bytesAt(STAGING.value())).isEqualTo(BYTES);
        assertThat(documentStore.descriptionAt(STAGING.value())).isEqualTo("application/samlmetadata+xml");
    }

    @Test
    @DisplayName("GIVEN an unreadable source WHEN stored THEN it fails with ContentUnreadable")
    public void storeFailsWhenSourceUnreadable() {

        ContentSource broken = () -> {

            throw new IOException("stream aborted");
        };

        assertThat(contentStore.store(STAGING, ContentType.none(), broken).getError())
            .isInstanceOf(ContentUnreadable.class);
    }

    @Test
    @DisplayName("GIVEN a staged document WHEN moved THEN it is renamed to the destination")
    public void moveRenamesToDestination() {

        contentStore.store(STAGING, ContentType.none(), ContentSource.ofBytes(BYTES));

        assertThat(contentStore.move(STAGING, DURABLE).isSuccess()).isTrue();
        assertThat(documentStore.hasDocument(DURABLE.value())).isTrue();
        assertThat(documentStore.hasDocument(STAGING.value())).isFalse();
    }

    @Test
    @DisplayName("GIVEN the destination already exists WHEN moved THEN it is idempotent")
    public void moveIsIdempotentWhenAlreadyAtDestination() {

        contentStore.store(DURABLE, ContentType.none(), ContentSource.ofBytes(BYTES));

        assertThat(contentStore.move(STAGING, DURABLE).isSuccess()).isTrue();
        assertThat(documentStore.hasDocument(DURABLE.value())).isTrue();
    }

    @Test
    @DisplayName("WHEN deleted THEN the document is removed; deleting an absent document still succeeds")
    public void deleteRemovesAndIsIdempotent() {

        contentStore.store(STAGING, ContentType.none(), ContentSource.ofBytes(BYTES));

        assertThat(contentStore.delete(STAGING).isSuccess()).isTrue();
        assertThat(documentStore.hasDocument(STAGING.value())).isFalse();
        assertThat(contentStore.delete(STAGING).isSuccess()).isTrue();
    }
}
