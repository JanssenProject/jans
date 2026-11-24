/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric.aggregation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@code jansData} of a counter aggregation row. {@code count} is the cluster-wide number of events in
 * the period. See {@link AggregateData} for the other properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "kind", "count", "sourceRows", "nodes", "complete", "legacyDelta" })
public class CounterAggregateData extends AggregateData {

    private static final long serialVersionUID = 2219461183245587431L;

    public static final String KIND = "counter";

    @Override
    public String getKind() {
        return KIND;
    }

}
