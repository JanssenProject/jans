package com.example.fido2.repository.dcr

import com.example.fido2.authAdaptor.DPoPProofFactoryProvider
import com.example.fido2.model.OIDCClient

interface DCRRepository {
    suspend fun getOIDCClient(dpoPProofFactory: DPoPProofFactoryProvider?): OIDCClient?
}