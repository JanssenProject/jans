package io.jans.configapi.plugin.facade.convert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public final class FacadeTemplates {

    private FacadeTemplates() {
    }

    public record Windows(long historySeconds, long dedupSeconds, long lookbackSeconds, long snapshotSeconds) {
    }

    public static String directive(List<String> peerAttrs, Windows windows) throws IOException {
        String peers = peerAttrs.stream().map(name -> peerFeatureConfig(name, windows.lookbackSeconds()))
                .collect(Collectors.joining("\n"));
        return load("directive.textproto.template")
                .replace("${history_seconds}", Long.toString(windows.historySeconds()))
                .replace("${dedup_seconds}", Long.toString(windows.dedupSeconds()))
                .replace("${lookback_seconds}", Long.toString(windows.lookbackSeconds()))
                .replace("${snapshot_seconds}", Long.toString(windows.snapshotSeconds()))
                .replace("${peer_feature_configs}", peers);
    }

    public static String config(List<String> peerAttrs, int trainingExamples) throws IOException {
        String segments = peerAttrs.stream().map(FacadeTemplates::contextSegment).collect(Collectors.joining("\n"));
        return load("config.textproto.template")
                .replace("${context_segments}", segments)
                .replace("${training_examples}", Integer.toString(trainingExamples));
    }

    private static String peerFeatureConfig(String name, long lookbackSeconds) {
        return """
                  peer_feature_configs {
                    name: "%s"
                    max_peers: 200
                    bipartite_graph {
                      traversal_modes: TM_UNDIRECTED
                      half_life {
                        seconds: %d
                      }
                      edge_weighting_method: EWM_DISCOUNTED_LATEST
                    }
                    aggregation_method: AGG_ACCUMULATE
                  }""".formatted(name, lookbackSeconds).indent(2).stripTrailing();
    }

    private static String contextSegment(String name) {
        return """
                    segment_reductions {
                      segment_weight_scaling: WS_IDENTITY
                      segment_weight_normalization: WN_L2
                      token_embedding_name: "context_principal"
                      token_feature_name: "%1$s/%2$s_u/t"
                      intensity_feature_name: "%1$s/%2$s_u/w"
                    }""".formatted(FacadeMapper.CONTEXT_TYPE, name).indent(4).stripTrailing();
    }

    private static String load(String name) throws IOException {
        try (InputStream in = FacadeTemplates.class.getResourceAsStream("/facade/" + name)) {
            if (in == null) {
                throw new IOException("missing resource /facade/" + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
