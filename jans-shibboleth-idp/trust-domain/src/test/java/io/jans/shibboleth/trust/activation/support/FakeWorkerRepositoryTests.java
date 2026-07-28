package io.jans.shibboleth.trust.activation.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.activation.error.WorkerNotFound;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FakeWorkerRepository — CRUD")
public class FakeWorkerRepositoryTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final WorkerId ID = WorkerId.of(Origin.of("instance@host")).getValue();

    private final FakeWorkerRepository repository = new FakeWorkerRepository();

    @Test
    @DisplayName("GIVEN a saved worker WHEN found by id THEN the same worker is returned")
    public void savesAndFinds() {

        Worker saved = Worker.register(ID, NOW).getValue();
        repository.save(saved);

        assertThat(repository.findById(ID).getValue()).isSameAs(saved);
    }

    @Test
    @DisplayName("GIVEN no such id WHEN found THEN it fails with WorkerNotFound")
    public void findByIdMissing() {

        Result<Worker> found = repository.findById(ID);

        assertThat(found.isFailure()).isTrue();
        assertThat(found.getError()).isInstanceOf(WorkerNotFound.class);
    }

    @Test
    @DisplayName("GIVEN a saved worker WHEN deleted THEN it can no longer be found")
    public void deletes() {

        repository.save(Worker.register(ID, NOW).getValue());

        repository.delete(ID);

        assertThat(repository.findById(ID).isFailure()).isTrue();
    }
}
