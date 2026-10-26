/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.jans.lock.service.trace.TraceConstants;

/**
 * Paths of Lock-derived members found in a signed body: every one is counted, the first
 * {@link TraceConstants#MAX_RESERVED_MEMBERS} are kept.
 */
final class ReservedMemberPaths {

	private final List<String> paths = new ArrayList<>();

	private int total;

	void add(String path) {
		total++;
		if (paths.size() < TraceConstants.MAX_RESERVED_MEMBERS) {
			paths.add(path);
		}
	}

	List<String> getPaths() {
		return Collections.unmodifiableList(paths);
	}

	int getTotal() {
		return total;
	}

}
