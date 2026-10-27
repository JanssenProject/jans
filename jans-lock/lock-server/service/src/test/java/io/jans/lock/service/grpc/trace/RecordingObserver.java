/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.trace;

import java.util.ArrayList;
import java.util.List;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/**
 * Records what a gRPC service implementation sends to its {@link StreamObserver}.
 */
class RecordingObserver<T> implements StreamObserver<T> {

	final List<T> values = new ArrayList<>();

	Throwable error;

	boolean completed;

	@Override
	public void onNext(T value) {
		values.add(value);
	}

	@Override
	public void onError(Throwable t) {
		error = t;
	}

	@Override
	public void onCompleted() {
		completed = true;
	}

	Status errorStatus() {
		return ((StatusRuntimeException) error).getStatus();
	}
}
