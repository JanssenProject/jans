/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.trust;

/**
 * Overall health of the FIDO Metadata Service data used for attestation validation.
 *
 * @author Janssen Project
 */
public enum MdsHealthStatus {

    /** Metadata is loaded and still inside its validity window. */
    UP,

    /** Metadata is expired, empty, or the last refresh failed. */
    DOWN,

    /** The metadata service is switched off by configuration. A choice, not a failure. */
    DISABLED
}
