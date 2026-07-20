package io.jans.shibboleth.trust.dto.activation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Read view of a registered worker: its identity ({@code origin} = "instance@host") and liveness
 * timestamps (ISO-8601 date-times).
 */
public class WorkerView {

    @JsonProperty("origin")
    private final String origin;

    @JsonProperty("registered_at")
    private final String registeredAt;

    @JsonProperty("last_heartbeat_at")
    private final String lastHeartbeatAt;

    public WorkerView(String origin, String registeredAt, String lastHeartbeatAt) {

        this.origin = origin;
        this.registeredAt = registeredAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public String getOrigin() {

        return origin;
    }

    public String getRegisteredAt() {

        return registeredAt;
    }

    public String getLastHeartbeatAt() {

        return lastHeartbeatAt;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkerView that = (WorkerView) o;
        return Objects.equals(origin, that.origin)
            && Objects.equals(registeredAt, that.registeredAt)
            && Objects.equals(lastHeartbeatAt, that.lastHeartbeatAt);
    }

    @Override
    public int hashCode() {

        return Objects.hash(origin, registeredAt, lastHeartbeatAt);
    }

    @Override
    public String toString() {

        return "WorkerView{origin='" + origin + "', registeredAt='" + registeredAt
            + "', lastHeartbeatAt='" + lastHeartbeatAt + "'}";
    }
}
