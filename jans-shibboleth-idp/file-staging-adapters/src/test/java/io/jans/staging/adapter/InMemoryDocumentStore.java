package io.jans.staging.adapter;

import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.provider.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal in-memory {@link DocumentStore} for adapter tests. Only the methods the content store uses are
 * implemented; the rest are unsupported. {@code saveBinaryDocumentStream} genuinely drains the stream, so
 * the content store's digest/count pass is exercised.
 */
final class InMemoryDocumentStore implements DocumentStore<Object> {

    private final Map<String, byte[]> bytesByPath = new HashMap<>();
    private final Map<String, String> descriptionByPath = new HashMap<>();

    @Override
    public boolean hasDocument(String path) {

        return bytesByPath.containsKey(path);
    }

    @Override
    public String saveBinaryDocumentStream(String path, String description, InputStream documentStream, String module) {

        try {

            bytesByPath.put(path, documentStream.readAllBytes());
            descriptionByPath.put(path, description);
            return path;
        } catch (IOException e) {

            throw new io.jans.service.document.store.exception.WriteDocumentException(e);
        }
    }

    @Override
    public boolean removeDocument(String path) {

        descriptionByPath.remove(path);
        return bytesByPath.remove(path) != null;
    }

    @Override
    public String renameDocument(String currentPath, String destinationPath) {

        byte[] bytes = bytesByPath.remove(currentPath);
        if (bytes == null) {

            return null;
        }
        bytesByPath.put(destinationPath, bytes);
        descriptionByPath.put(destinationPath, descriptionByPath.remove(currentPath));
        return destinationPath;
    }

    byte[] bytesAt(String path) {

        return bytesByPath.get(path);
    }

    String descriptionAt(String path) {

        return descriptionByPath.get(path);
    }

    @Override
    public String saveDocument(String path, String description, String content, Charset charset, String module) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String saveDocumentStream(String path, String description, InputStream stream, String module) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String readDocument(String path, Charset charset) {

        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream readDocumentAsStream(String path) {

        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream readBinaryDocumentAsStream(String path) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> findDocumentsByModules(List<String> moduleList, String... attributes) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentStoreType getProviderType() {

        return null;
    }
}
