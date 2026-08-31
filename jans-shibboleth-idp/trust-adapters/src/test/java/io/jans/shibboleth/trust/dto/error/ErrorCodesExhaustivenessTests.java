package io.jans.shibboleth.trust.dto.error;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.kernel.DomainError;
import io.jans.kernel.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Guards the boundary against a domain error type nobody taught it about.
 *
 * <p>Without this, adding a domain error is silently lossy: it reaches clients as
 * {@link io.jans.adapter.error.KernelErrorCodes#UNEXPECTED} with prose that says nothing, and no
 * test fails. The types are discovered by scanning rather than listed here on purpose — a
 * hand-maintained list in a test goes stale exactly as easily as the registry it is meant to check.
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
     * The boundary's own package. {@link io.jans.adapter.error.RequestValidationFailed} is the
     * envelope that carries violations rather than a failure needing a code of its own, so it is not
     * registered and must not be demanded here.
     */
    private static final String BOUNDARY_PACKAGE = "io.jans.adapter.error.";

    @Test
    @DisplayName("GIVEN every concrete DomainError on the classpath WHEN checked THEN each has a registered code")
    public void everyConcreteDomainErrorIsRegistered() {

        List<Class<? extends DomainError>> discovered = discoverConcreteDomainErrors();

        // A scan that reaches nothing would pass vacuously, which is worse than failing. Naming one
        // known type per scanned module proves the scan reached each of them, whether they are on
        // the classpath as directories (full reactor build) or as jars (single-module build, IDE,
        // per-module CI) — a size threshold alone silently tolerated finding neither.
        assertThat(discovered)
            .as("concrete DomainError types discovered on the classpath")
            .contains(RequiredValueMissing.class, InvalidUriSyntax.class, StaleReport.class)
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

    @SuppressWarnings("unchecked")
    private static List<Class<? extends DomainError>> discoverConcreteDomainErrors() {

        List<Class<? extends DomainError>> found = new ArrayList<>();

        for (Class<?> candidate : ClasspathClasses.under(SCANNED_PACKAGES)) {

            boolean concreteDomainError = DomainError.class.isAssignableFrom(candidate)
                && !candidate.equals(DomainError.class)
                && !Modifier.isAbstract(candidate.getModifiers())
                && !candidate.isAnonymousClass()
                && !candidate.isSynthetic()
                && !candidate.getName().startsWith(BOUNDARY_PACKAGE);

            if (concreteDomainError) {

                found.add((Class<? extends DomainError>) candidate);
            }
        }

        return found;
    }
}
