package io.jans.shibboleth.trust.dto.mapper.activation;

import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.dto.activation.RegisterWorkerRequest;
import io.jans.shibboleth.trust.dto.activation.WorkerView;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.kernel.FieldPath;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;
import io.jans.shibboleth.trust.dto.error.Violations;

/**
 * Translates between the domain {@link Worker} / {@link WorkerId} and their DTOs.
 */
public final class WorkerMapper {

    private WorkerMapper() {
    }

    /**
     * Builds a {@link WorkerId} from a register request. A blank/absent origin is rejected — a worker
     * must present a real identity.
     */
    public static Result<WorkerId> toWorkerId(RegisterWorkerRequest request) {

        return toWorkerId(request.getOrigin());
    }

    /**
     * Builds a {@link WorkerId} from a presented origin (e.g. a {@code {worker}} path segment). A
     * blank/absent origin is rejected — a worker must present a real identity.
     */
    public static Result<WorkerId> toWorkerId(String origin) {

        Violations violations = Violations.create();

        if (origin == null || origin.isBlank()) {

            violations.record(RequiredValueMissing.of(WorkerId.class), FieldPath.of("origin"));

            return violations.asFailure();
        }

        return violations.completeWith(WorkerId.of(Origin.of(origin)).at("origin"));
    }

    /**
     * Projects a registered {@link Worker} onto its read view.
     */
    public static WorkerView toView(Worker worker) {

        return new WorkerView(
            worker.id().origin().getValue(),
            worker.registeredAt().toString(),
            worker.lastHeartbeatAt().toString());
    }
}
