package io.jans.shibboleth.trust.dto.mapper.activation;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.dto.activation.RegisterWorkerRequest;
import io.jans.shibboleth.trust.dto.activation.WorkerView;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class WorkerMapperTests {

    @Test
    void shouldBuildWorkerIdFromOrigin() {

        Result<WorkerId> result = WorkerMapper.toWorkerId(new RegisterWorkerRequest("worker-1@host"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().origin().getValue()).isEqualTo("worker-1@host");
    }

    @Test
    void shouldFailWhenOriginIsBlank() {

        Result<WorkerId> result = WorkerMapper.toWorkerId(new RegisterWorkerRequest("   "));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldProjectWorkerToView() {

        Instant registeredAt = Instant.parse("2027-01-01T00:00:00Z");
        Worker worker = Worker.register(WorkerId.of(Origin.of("worker-1@host")).getValue(), registeredAt).getValue();

        WorkerView view = WorkerMapper.toView(worker);

        assertThat(view.getOrigin()).isEqualTo("worker-1@host");
        assertThat(view.getRegisteredAt()).isEqualTo("2027-01-01T00:00:00Z");
        assertThat(view.getLastHeartbeatAt()).isEqualTo("2027-01-01T00:00:00Z");
    }
}
