//
//  Authenticator.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.07.2024.
//

import Foundation
import CommonCrypto

final class Authenticator {
    
    private let credentialSafe = CredentialSafe()
    private let cryptoProvider = WebAuthnCryptography()
    
    func getPublicKeyCredentialSource(options: AuthenticatorMakeCredentialOptions, passwordText: String) -> PublicKeyCredentialSource {
        let credentialSource = credentialSafe.generateCredential(
            rpEntityId: options.rpEntity.id,
            userHandle: options.userEntity?.id ?? Data(),
            userDisplayName: options.userEntity?.name ?? "none", 
            passwordText: passwordText
        )
        
        return credentialSource
    }
    
    func generateAuthenticatorMakeCredentialOptions(responseFromAPI: AttestationOptionResponse?, origin: String?) -> AuthenticatorMakeCredentialOptions {
        var options = AuthenticatorMakeCredentialOptions()
        options.rpEntity = RpEntity()
        options.rpEntity.id = responseFromAPI?.rp?.id ?? ""
        options.rpEntity.name = responseFromAPI?.rp?.name ?? ""
        options.userEntity = UserEntity()
        options.userEntity?.id = responseFromAPI?.user?.id.data(using: .utf8, allowLossyConversion: true) ?? Data()
        options.userEntity?.name = responseFromAPI?.user?.name ?? ""
        options.userEntity?.displayName = responseFromAPI?.user?.displayName ?? ""
        options.clientDataHash = generateClientDataHash(
            challenge: responseFromAPI?.challenge,
            type: "webauthn.create",
            origin: origin
        ) ?? Data()
        options.requireResidentKey = false
        options.requireUserPresence = true
        options.requireUserVerification = false
        options.excludeCredentialDescriptorList = [PublicKeyCredentialDescriptor()]
        let credTypesAndPubKeyAlgs: [String: Int] = ["public-key": 7]
        options.credTypesAndPubKeyAlgs = credTypesAndPubKeyAlgs
        return options
    }
    
    func generateAuthenticatorGetAssertionOptions(assertionOptionResponse: AssertionOptionResponse?, origin: String?) -> AuthenticatorGetAssertionOptions {
        var options = AuthenticatorGetAssertionOptions()
        options.rpId = assertionOptionResponse?.rpId
        options.requireUserVerification = false
        options.requireUserPresence = true
        options.clientDataHash = generateClientDataHash(
            challenge: assertionOptionResponse?.challenge,
            type: "webauthn.get",
            origin: origin
        ) ?? Data()
        let allowCredentialDescriptorList = assertionOptionResponse?.allowCredentials?.map {
            PublicKeyCredentialDescriptor(
                id: $0.id.data(using: .utf8) ?? Data(),
                type: $0.type,
                transports: $0.transports
            )
        }
        options.allowCredentialDescriptorList = allowCredentialDescriptorList
        return options
    }
    
    func register(responseFromAPI: AttestationOptionResponse?, origin: String?, credentialSource: PublicKeyCredentialSource?) -> AttestationResultRequest? {
        let options = generateAuthenticatorMakeCredentialOptions(responseFromAPI: responseFromAPI, origin: origin)
        
        let attestationObject = makeInternalCredential(options: options, credentialSource: credentialSource)
        
        let clientDataJSON = generateClientDataJSON(challenge: responseFromAPI?.challenge, type: "webauthn.create", origin: origin) ?? ""
        let attestationObjectBytes = attestationObject.asCBOR
        let response = AttestationDataResponse(attestationObject: attestationObjectBytes.base64EncodedString(), clientDataJSON: clientDataJSON)
        let attestationResultRequest = AttestationResultRequest(type: "public-key", response: response)
        
        return attestationResultRequest
    }
    
    func authenticate(assertionOptionResponse: AssertionOptionResponse, origin: String?, selectedCredential: PublicKeyCredentialSource?) -> AssertionResultRequest {
        let options: AuthenticatorGetAssertionOptions = generateAuthenticatorGetAssertionOptions(assertionOptionResponse: assertionOptionResponse, origin: origin)
        var assertionResultRequest = AssertionResultRequest()
        
        guard let selectedCredential else {
            return assertionResultRequest
        }
        
        let assertionObject: AuthenticatorGetAssertionResult? = getAssertion(options: options, selectedCredential: selectedCredential, credentialSelector: LocalCredentialSelector())

        assertionResultRequest.id = assertionObject?.selectedCredentialId?.base64EncodedString().replacingOccurrences(of: "\n", with: "").replacingOccurrences(of: "=", with: "")
        assertionResultRequest.type = "public-key"
        assertionResultRequest.rawId = assertionObject?.selectedCredentialId?.base64EncodedString().replacingOccurrences(of: "\n", with: "")

        var response = Response()
        response.clientDataJSON = generateClientDataJSON(
            challenge: assertionOptionResponse.challenge,
            type: "webauthn.get",
            origin: origin)

        response.authenticatorData = assertionObject?.authenticatorData?.base64EncodedString().replacingOccurrences(of: "\n", with: "")
        
        response.signature = assertionObject?.signature?.base64EncodedString().replacingOccurrences(of: "\n", with: "")
        
        assertionResultRequest.response = response
        
        return assertionResultRequest
    }
    
    func selectPublicKeyCredentialSource(credentialSelector: CredentialSelector, options: AuthenticatorGetAssertionOptions) -> PublicKeyCredentialSource? {
        var credentials: [PublicKeyCredentialSource] = credentialSafe.getKeysForEntity(rpEntityId: options.rpId ?? "")
        
        if let allowCredentialDescriptorList = options.allowCredentialDescriptorList {
            var filteredCredentials: [PublicKeyCredentialSource] = []
            
            allowCredentialDescriptorList.forEach { allowCredential in
                credentials.forEach { credential in
                    if credential.id == allowCredential.id {
                        filteredCredentials.append(credential)
                    }
                }
            }
            
            if !filteredCredentials.isEmpty {
                credentials = filteredCredentials
            }
        }
        
        if credentials.count == 1 {
            return credentials.first
        }
        
        return credentialSelector.selectFrom(credentialList: credentials)
    }
    
    func generateSignature(credentialSource: PublicKeyCredentialSource) {
        WebAuthnCryptography().generateSignatureObject(alias: credentialSource.keyPairAlias)
    }
    
    func getAssertion(options: AuthenticatorGetAssertionOptions, selectedCredential: PublicKeyCredentialSource, credentialSelector: CredentialSelector) -> AuthenticatorGetAssertionResult? {
        
        // 1. Check if all supplied parameters are well-formed
        if !options.areWellFormed() {
            return nil
        }

        // 2-3. Parse allowCredentialDescriptorList
        // we do this slightly out of order, see below.

        // 4-5. Get keys that match this relying party ID

        // get verification, if necessary
        var result = AuthenticatorGetAssertionResult()
        let keyNeedsUnlocking: Bool = credentialSafe.keyRequiresVerification(alias: selectedCredential.keyPairAlias)
        if options.requireUserVerification == true || keyNeedsUnlocking {
            result = getInternalAssertion(options: options, selectedCredential: selectedCredential)
        } else { // no biometric
            // steps 8-13
            result = getInternalAssertion(options: options, selectedCredential: selectedCredential)
        }
        return result;
    }
    
    // MARK: - Private
    
    private func getInternalAssertion(options: AuthenticatorGetAssertionOptions, selectedCredential: PublicKeyCredentialSource) -> AuthenticatorGetAssertionResult {
        var authenticatorData = Data()
        var signatureBytes = Data()
        
        // 9. Increment signature counter
        let authCounter = credentialSafe.incrementCredentialUseCounter(credential: selectedCredential)
        if let rpId = options.rpId {
            let rpIdHash = cryptoProvider.sha256(string: rpId)
            authenticatorData = constructAuthenticatorData(rpIdHash: rpIdHash, attestedCredentialData: nil, authCounter: authCounter) ?? Data()
        }
        
        let result = AuthenticatorGetAssertionResult(
            selectedCredentialId: selectedCredential.id,
            authenticatorData: authenticatorData,
            signature: signatureBytes,
            selectedCredentialUserHandle: selectedCredential.userHandle)
        
        return result
    }
    
    private func generateClientDataHash(challenge: String?, type: String?, origin: String?) -> Data? {
        // Convert clientDataJson to JSON string
        let dictionary = ["type": type, "challenge": challenge, "origin": origin]
        let encoder = JSONEncoder()
        if let jsonData = try? encoder.encode(dictionary) {
            // Calculate SHA-256 hash
            return sha256(data: jsonData)
        }
        
        return nil
    }
    
    private func constructAuthenticatorData(rpIdHash: Data, attestedCredentialData: Data?, authCounter: Int) -> Data? {
        if rpIdHash.count != 32 {
            print("rpIdHash must be a 32-byte SHA-256 hash")
            return nil
        }

        //            byte flags = 0x00;
        //            flags |= 0x01; // user present
        //            if (this.credentialSafe.supportsUserVerification()) {
        //                flags |= (0x01 << 2); // user verified
        //            }
        //            if (attestedCredentialData != null) {
        //                flags |= (0x01 << 6); // attested credential data included
        //            }
        //
        //            // 32-byte hash + 1-byte flags + 4 bytes signCount = 37 bytes
        //            ByteBuffer authData = ByteBuffer.allocate(37 +
        //                    (attestedCredentialData == null ? 0 : attestedCredentialData.length));
        var authData = Data()
        authData.append(rpIdHash)
        //        authData.put(flags);
        authCounter.description.data(using: .utf8).flatMap {
            authData.append($0)
        }
        attestedCredentialData.flatMap {
            authData.append($0)
        }
        
        return authData
    }
    
    private func sha256(data: Data) -> Data {
        var hash = [UInt8](repeating: 0,  count: Int(CC_SHA256_DIGEST_LENGTH))
        data.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash)
        }
        return Data(hash)
    }
    
    private func makeInternalCredential(options: AuthenticatorMakeCredentialOptions, credentialSource: PublicKeyCredentialSource?) -> AttestationObject {
        AttestationObject(authData: Data())
    }
    
    private func generateClientDataJSON(challenge: String?, type: String?, origin: String?) -> String? {
        let clientData = ClientData(type: type, challenge: challenge, origin: origin)
        
        let jsonEncoder = JSONEncoder()
        if let jsonData = try? jsonEncoder.encode(clientData) {
            return String(data: jsonData, encoding: String.Encoding.utf8)
        }
        
        return nil
    }
}
