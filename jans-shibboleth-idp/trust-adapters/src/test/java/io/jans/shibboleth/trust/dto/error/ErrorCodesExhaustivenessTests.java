package io.jans.shibboleth.trust.dto.error;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.kernel.DomainError;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Guards the boundary against a domain error type nobody taught it about.
 *
 * <p>Without this, adding a domain error is silently lossy: it reaches clients as
 * {@link ErrorCodes#UNEXPECTED} with prose that says nothing, and no test fails. The types are
 * discovered by scanning rather than listed here on purpose — a hand-maintained list in a test goes
 * stale exactly as easily as the registry it is meant to check.
 */
@DisplayName("ErrorCodes — every domain error type has a client-facing code")
public class ErrorCodesExhaustivenessTests {

    /**
     * The contexts this boundary serves. File staging is a separate bounded context with its own
     * adapters, so its errors are deliberately out of scope here.
     */
    private static final List<String> SCANNED_PACKAGES = Arrays.asList(
        "io/jans/kernel",
        "io/jans/shibboleth/trust");

    /**
     * The boundary's own package. {@link RequestValidationFailed} is the envelope that carries
     * violations rather than a failure needing a code of its own, so it is not registered and must
     * not be demanded here.
     */
    private static final String BOUNDARY_PACKAGE = "io.jans.shibboleth.trust.dto.error.";

    @Test
    @DisplayName("GIVEN every concrete DomainError on the classpath WHEN checked THEN each has a registered code")
    public void everyConcreteDomainErrorIsRegistered() {

        List<Class<? extends DomainError>> discovered = discoverConcreteDomainErrors();

        // a scan that finds nothing would pass vacuously, which is worse than failing
        assertThat(discovered)
            .as("concrete DomainError types discovered on the classpath")
            .hasSizeGreaterThan(15);

        List<String> unregistered = new ArrayList<>();
        for (Class<? extends DomainError> type : discovered) {

            if (!ErrorCodes.isRegistered(type)) {

                unregistered.add(type.getName());
            }
        }

        assertThat(unregistered)
            .as("domain error types with no code in ErrorCodes — add them there, "
                + "and decide whether each is field-scoped")
            .isEmpty();
    }

    @Test
    @DisplayName("GIVEN the registry WHEN codes are read THEN each is a distinct snake_case token")
    public void codesAreDistinctAndWellFormed() {

        List<String> codes = new ArrayList<>();
        for (Class<? extends DomainError> type : ErrorCodes.registeredTypes()) {

            codes.add(ErrorCodes.codeFor(type));
        }

        assertThat(codes).doesNotHaveDuplicates();
        assertThat(codes).allMatch(code -> code.matches("[a-z][a-z0-9_]*"), "snake_case");
        assertThat(codes).doesNotContain(ErrorCodes.UNEXPECTED);
    }

    private static List<Class<? extends DomainError>> discoverConcreteDomainErrors() {

        List<Class<? extends DomainError>> found = new ArrayList<>();

        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {

            Path root = Path.of(entry);
            if (!Files.isDirectory(root)) {

                continue;
            }

            for (String scanned : SCANNED_PACKAGES) {

                Path packageRoot = root.resolve(scanned);
                if (Files.isDirectory(packageRoot)) {

                    collect(root, packageRoot, found);
                }
            }
        }

        found.sort(Comparator.comparing(Class::getName));
        return found;
    }

    private static void collect(Path root, Path packageRoot, List<Class<? extends DomainError>> found) {

        try (Stream<Path> files = Files.walk(packageRoot)) {

            files.filter(path -> path.toString().endsWith(".class"))
                .forEach(path -> {

                    String className = root.relativize(path).toString()
                        .replace(File.separatorChar, '.')
                        .replaceAll("\\.class$", "");

                    Class<? extends DomainError> type = asConcreteDomainError(className);
                    if (type != null) {

                        found.add(type);
                    }
                });
        } catch (IOException e) {

            throw new IllegalStateException("Could not scan " + packageRoot + " for domain errors", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends DomainError> asConcreteDomainError(String className) {

        try {

            Class<?> candidate =
                Class.forName(className, false, ErrorCodesExhaustivenessTests.class.getClassLoader());

            boolean concreteDomainError = DomainError.class.isAssignableFrom(candidate)
                && !candidate.equals(DomainError.class)
                && !Modifier.isAbstract(candidate.getModifiers())
                && !candidate.isAnonymousClass()
                && !candidate.isSynthetic()
                && !candidate.getName().startsWith(BOUNDARY_PACKAGE);

            return concreteDomainError ? (Class<? extends DomainError>) candidate : null;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {

            return null;
        }
    }
}
