/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.service.mds;

// https://fidoalliance.org/specs/fido-v2.0-rd-20180702/fido-metadata-service-v2.0-rd-20180702.html
public enum AuthenticatorStatus {
    NOT_FIDO_CERTIFIED, FIDO_CERTIFIED, USER_VERIFICATION_BYPASS, ATTESTATION_KEY_COMPROMISE, USER_KEY_REMOTE_COMPROMISE, USER_KEY_PHYSICAL_COMPROMISE, UPDATE_AVAILABLE, REVOKED, SELF_ASSERTION_SUBMITTED, FIDO_CERTIFIED_L1, FIDO_CERTIFIED_L1plus, FIDO_CERTIFIED_L2, FIDO_CERTIFIED_L2plus, FIDO_CERTIFIED_L3, FIDO_CERTIFIED_L3plus
}
