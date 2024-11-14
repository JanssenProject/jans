//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

protocol Authenticator {
    var type: AuthenticatorType { get }
    var credSrcStorage: CredentialSourceStorage { get }
    var keyStorage: KeyStorage { get }
    var localAuthn: LocalAuthenticationProtocol { get }

    func makeCredential(
        hash: Data,
        rpEntity: PublicKeyCredentialRpEntity,
        userEntity: PublicKeyCredentialUserEntity,
        credTypesAndPubKeyAlgs: [PublicKeyCredentialParameters],
        excludeCredentialDescriptorList: [PublicKeyCredentialDescriptor]?,
        extensions: AuthenticatorExtensionsInput?
    ) async -> Result<AuthenticatorMakeCredentialResult, WebAuthnError>

    func getAssertion(
        rpId: String,
        hash: Data,
        allowCredentialDescriptorList: [PublicKeyCredentialDescriptor]?,
        extensions: AuthenticatorExtensionsInput?
    ) async -> Result<AuthenticatorGetAssertionResult, WebAuthnError>
}

extension Authenticator {
    private func checkCredTypesAndPubKeyAlgsSupported(_ credTypesAndPubKeyAlgs: [PublicKeyCredentialParameters]
    ) -> PublicKeyCredentialParameters? {
        return credTypesAndPubKeyAlgs.first(where: { $0.type == "public-key" && $0.alg == .ES256 })
    }

    private func isNotRegistered(_ rpId: String, _ credList: [PublicKeyCredentialDescriptor]?
    ) -> Result<Bool, WebAuthnError> {
        guard let credList = credList else {
            return .success(true)
        }
        for cred in credList {
            let result = credSrcStorage.load(cred.id)
            switch result {
            case .failure(let err):
                return .failure(.credSrcStorageError(err, credId: cred.id))
            case .success(let credSrc):
                if let credSrc = credSrc, credSrc.rpId == rpId, credSrc.type == cred.type {
                    return .success(false)
                }
            }
        }
        return .success(true)
    }

    private func checkAllowedCredentials(_ rpId: String, _ allowedList: [PublicKeyCredentialDescriptor]?
    ) -> Result<[PublicKeyCredentialSource], WebAuthnError> {
        var credentialOptions: [PublicKeyCredentialSource] = []
        if let allowedList = allowedList {
            for descriptor in allowedList {
                let result = credSrcStorage.load(descriptor.id)
                switch result {
                case .failure(let err):
                    return .failure(.credSrcStorageError(err, credId: descriptor.id))
                case .success(let credSrc):
                    if let credSrc = credSrc, rpId == credSrc.rpId {
                        credentialOptions.append(credSrc)
                    }
                }
            }
        } else {
            let result = credSrcStorage.loadAll()
            switch result {
            case .failure(let err):
                return .failure(.credSrcStorageError(err))
            case .success(let credSrcs):
                if let credSrcs = credSrcs {
                    for credSrc in credSrcs where rpId == credSrc.rpId {
                        credentialOptions.append(credSrc)
                    }
                }
            }
        }
        return .success(credentialOptions)
    }

    func makeCredential(hash: Data,
                        rpEntity: PublicKeyCredentialRpEntity,
                        userEntity: PublicKeyCredentialUserEntity,
                        credTypesAndPubKeyAlgs: [PublicKeyCredentialParameters],
                        excludeCredentialDescriptorList: [PublicKeyCredentialDescriptor]?,
                        extensions: AuthenticatorExtensionsInput?
    ) async -> Result<AuthenticatorMakeCredentialResult, WebAuthnError> {
        do {
            guard let matchedKeyParam = checkCredTypesAndPubKeyAlgsSupported(credTypesAndPubKeyAlgs) else {
                let msg = "Currently there is no supported algorithm: \(credTypesAndPubKeyAlgs)"
                throw WebAuthnError.coreError(.notSupportedError, cause: msg)
            }
            let isNotRegistered = try isNotRegistered(rpEntity.id, excludeCredentialDescriptorList).get()
            guard isNotRegistered else {
                throw WebAuthnError.coreError(.invalidStateError)
            }
            guard try await localAuthn.execute().get() else {
                throw WebAuthnError.coreError(.notAllowedError)
            }
            guard let credentialId = generateRandomBytes(len: 32) else {
                throw WebAuthnError.utilityError(cause: "Failed to generate random bytes")
            }
            let credIdStr = credentialId.toBase64Url()
            let credSource = PublicKeyCredentialSource(
                id: credIdStr,
                type: matchedKeyParam.type,
                aaguid: type.uuidString,
                userId: userEntity.id,
                rpId: rpEntity.id,
                userName: userEntity.name,
                userDisplayName: userEntity.displayName
            )
            let priKey = try generatePublicPrivateKeyPair(matchedKeyParam.alg.keyType, matchedKeyParam.alg.keyLen).get()
            guard let pubKey = getPublicKey(priKey) else {
                throw WebAuthnError.keyNotFoundError
            }
            let cborPubKey = try convertSecKeyToCborEc2coseKey(pubKey).get()
            let attestedCredData = AttestedCredentialData(aaguid: type.aaguid, credentialId: credentialId,
                                                          publicKey: cborPubKey)
            let rpIdHash = rpEntity.id.toSHA256()
            let authData = AuthenticatorData(rpIdHash, true, true, UInt32(0), attestedCredData,
                                             extensions?.processAuthenticatorExtensions())
            let attestation = NoneAttestationObject(authData: authData.toData())
            let cborAttestation = try attestation.toCBOR().get()
            _ = try credSrcStorage.store(credSource).mapError { e in
                WebAuthnError.credSrcStorageError(e, credId: credSource.id)
            }.get()
            _ = try keyStorage.store(credSource.id, key: priKey).mapError { e in
                WebAuthnError.keyStorageError(e, credId: credSource.id)
            }.get()
            return .success(AuthenticatorMakeCredentialResult(credentialId: credentialId,
                                                              attestationObject: cborAttestation))
        } catch {
            guard let error = error as? WebAuthnError else {
                return .failure(.unknownError(error))
            }
            return .failure(error)
        }
    }

    func getAssertion(rpId: String,
                      hash: Data,
                      allowCredentialDescriptorList: [PublicKeyCredentialDescriptor]?,
                      extensions: AuthenticatorExtensionsInput?
    ) async -> Result<AuthenticatorGetAssertionResult, WebAuthnError> {
        do {
            let credentialOptions = try checkAllowedCredentials(rpId, allowCredentialDescriptorList).get()
            guard !credentialOptions.isEmpty else {
                throw WebAuthnError.coreError(.notAllowedError)
            }
            let selectedCredential = credentialOptions[0]
            let credId = selectedCredential.id
            let rpIdHash = rpId.toSHA256()
            let key = try keyStorage.load(credId).mapError { e in
                WebAuthnError.keyStorageError(e, credId: credId)
            }.get()
            guard let key = key else {
                throw WebAuthnError.keyNotFoundError
            }
            let keyAlgorithm = try getKeyAlgorithm(key).get()
            let signCount = try credSrcStorage.increaseSignatureCounter(credId).mapError { e in
                WebAuthnError.credSrcStorageError(e, credId: credId)
            }.get()
            let authData = AuthenticatorData(rpIdHash, true, true, signCount, nil,
                                             extensions?.processAuthenticatorExtensions()).toData()
            let signature = try sign(key, keyAlgorithm, authData + hash).get()
            guard let credId = credId.base64UrlToData() else {
                throw WebAuthnError.utilityError(cause: "Failed to convert base64URL string into Data: \(credId)")
            }
            let userId: String? = if let userId = selectedCredential.userId { userId } else { nil }
            return .success(AuthenticatorGetAssertionResult(userHandle: userId, credentialId: credId,
                                                            authenticatorData: authData, signature: signature))
        } catch {
            guard let error = error as? WebAuthnError else {
                return .failure(.unknownError(error))
            }
            return .failure(error)
        }
    }
}
