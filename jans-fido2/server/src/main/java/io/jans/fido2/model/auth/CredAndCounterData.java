/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.model.auth;

public class CredAndCounterData {
    private String credId;
    private int counters;
    private String attestationType;
    private String uncompressedEcPoint;
    private int signatureAlgorithm;
    private boolean backupStateFlag;
    private boolean backupEligibilityFlag;

    public String getCredId() {
        return credId;
    }

    public int getCounters() {
        return counters;
    }

    public CredAndCounterData setCredId(String credId) {
        this.credId = credId;
        return this;
    }

    public CredAndCounterData setCounters(int counters) {
        this.counters = counters;
        return this;
    }

    public CredAndCounterData setAttestationType(String attestationType) {
        this.attestationType = attestationType;
        return this;
    }

    public String getAttestationType() {
        return attestationType;
    }

    public CredAndCounterData setUncompressedEcPoint(String uncompressedEcPoint) {
        this.uncompressedEcPoint = uncompressedEcPoint;
        return this;
    }

    public String getUncompressedEcPoint() {
        return uncompressedEcPoint;
    }

    public int getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(int signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public boolean getBackupStateFlag() {
        return backupStateFlag;
    }

    public void setBackupStateFlag(boolean backupStateFlag) {
        this.backupStateFlag = backupStateFlag;
    }

    public boolean getBackupEligibilityFlag() {
        return backupEligibilityFlag;
    }

    public void setBackupEligibilityFlag(boolean backupEligibilityFlag) {
        this.backupEligibilityFlag = backupEligibilityFlag;
    }
}
