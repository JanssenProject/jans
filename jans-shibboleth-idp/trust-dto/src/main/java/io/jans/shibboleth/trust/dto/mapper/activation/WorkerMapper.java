package io.jans.shibboleth.trust.dto.mapper.activation;

import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.dto.activation.RegisterWorkerRequest;
import io.jans.shibboleth.trust.dto.activation.WorkerView;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

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

        if (request.getOrigin() == null || request.getOrigin().isBlank()) {

            return Result.failure(RequiredValueMissing.forField("origin"));
        }

        return WorkerId.of(Origin.of(request.getOrigin()));
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
