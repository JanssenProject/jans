package io.jans.shibboleth.trust.activation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group 12 — Activation Boundary Guards")
public class ActivationBoundaryTests {

    private static List<String> importsUnder(String subPackage) throws IOException {

        Path dir = Paths.get("src", "main", "java", "io", "jans", "shibboleth", "trust", "activation", subPackage);
        assertThat(Files.isDirectory(dir)).as("expected package directory %s to exist", dir).isTrue();

        List<Path> javaFiles;
        try (Stream<Path> files = Files.walk(dir)) {
            javaFiles = files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList());
        }

        List<String> imports = new ArrayList<>();
        for (Path file : javaFiles) {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("import ")) {
                    imports.add(trimmed);
                }
            }
        }
        return imports;
    }

    private static List<String> activationDomainImports() throws IOException {

        List<String> imports = new ArrayList<>(importsUnder("model"));
        imports.addAll(importsUnder("workers"));
        return imports;
    }

    @Test
    @DisplayName("GIVEN the activation model and workers packages WHEN their imports are scanned THEN they depend on no trust-context type except the ActivationDiagnostics finalize contract")
    public void shouldNotDependOnTrustContext_fromActivationDomain() throws IOException {

        List<String> trustImports = activationDomainImports().stream()
            .filter(i -> i.contains("io.jans.shibboleth.trust.config."))
            .filter(i -> !i.contains("io.jans.shibboleth.trust.config.diagnostics."))
            .collect(Collectors.toList());

        assertThat(trustImports).isEmpty();
    }

    @Test
    @DisplayName("GIVEN the activation domain WHEN its TR references are inspected THEN the trust Id type appears nowhere in the domain")
    public void shouldReferenceTrOnlyByOpaqueValue() throws IOException {

        assertThat(activationDomainImports())
            .noneMatch(i -> i.contains("io.jans.shibboleth.trust.config.Id;"));
    }
}
