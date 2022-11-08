/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.mds;

/**
 * This enumeration describes the status of an authenticator model as identified by its AAID and potentially some additional information (such as a specific attestation key). -https://fidoalliance.org/specs/fido-v2.0-rd-20180702/fido-metadata-service-v2.0-rd-20180702.html 
 *
 */
public enum AuthenticatorCertificationStatus {

	NOT_FIDO_CERTIFIED, FIDO_CERTIFIED, USER_VERIFICATION_BYPASS, ATTESTATION_KEY_COMPROMISE, USER_KEY_REMOTE_COMPROMISE, USER_KEY_PHYSICAL_COMPROMISE, UPDATE_AVAILABLE, REVOKED, SELF_ASSERTION_SUBMITTED, FIDO_CERTIFIED_L1, FIDO_CERTIFIED_L1plus, FIDO_CERTIFIED_L2, FIDO_CERTIFIED_L2plus, FIDO_CERTIFIED_L3, FIDO_CERTIFIED_L3plus

}
