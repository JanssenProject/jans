package io.jans.configapi.plugin.facade.convert;

import java.util.List;

public final class FacadeRows {

    private FacadeRows() {
    }

    public record Action(String type, String id, String resourceId, String principal, long occurredAt,
            String historyKey) {
    }

    public record Peer(String name, String value, String direction, double weight) {
    }

    public record Context(String type, String principal, long validFrom, List<Peer> peers) {
    }
}
