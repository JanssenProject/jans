/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.entity;

/**
 * State of a TRACE receipt-chain position allocation claim (design §9, TRACE MVP design decision
 * D-8). Stored on {@link TraceReceiptEntry#getReceiptState()} as its {@code String} name.
 *
 * @author Yuriy Movchan
 */
public enum TraceReceiptState {

    PENDING,
    COMMITTED,
    VOID;

}
