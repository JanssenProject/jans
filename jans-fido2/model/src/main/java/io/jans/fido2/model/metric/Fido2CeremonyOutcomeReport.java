/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.metric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What the browser observed when an assertion ceremony ended without an assertion being produced.
 * <p>
 * The relying party cannot see why a ceremony was given up on: user verification happens inside the
 * authenticator, and the page only receives a {@code DOMException}. This carries the two things the
 * browser does know — which exception it was, and how long the user spent before it arrived — so a
 * deliberate opt-out can be told apart from repeated verification failures.
 * <p>
 * Every field is untrusted client input. It annotates an existing ceremony and nothing more: it can
 * never create one, change its status, or influence whether an authentication succeeds.
 *
 * @author Janssen Project
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2CeremonyOutcomeReport {

    /** The challenge the ceremony was issued with. Identifies which ceremony this describes. */
    private String challenge;

    /** The {@code DOMException} name, e.g. {@code NotAllowedError} or {@code AbortError}. */
    private String errorName;

    /** Milliseconds between the ceremony starting in the browser and the exception arriving. */
    private long elapsedMs;

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getErrorName() {
        return errorName;
    }

    public void setErrorName(String errorName) {
        this.errorName = errorName;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    @Override
    public String toString() {
        return "Fido2CeremonyOutcomeReport [errorName=" + errorName + ", elapsedMs=" + elapsedMs + "]";
    }
}
