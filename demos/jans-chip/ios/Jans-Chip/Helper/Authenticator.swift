//
//  Authenticator.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.07.2024.
//

import Foundation
import CommonCrypto

final class Authenticator {
    
    func getPublicKeyCredentialSource(options: AuthenticatorMakeCredentialOptions, passwordText: String) -> PublicKeyCredentialSource {
        let credentialSafe = CredentialSafe()
        let credentialSource = credentialSafe.generateCredential(
            rpEntityId: options.rpEntity.id,
            userHandle: options.userEntity?.id ?? Data(),
            userDisplayName: options.userEntity?.name ?? "admin", 
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
        options.userEntity?.id = responseFromAPI?.user?.id.data(using: .utf8, allowLossyConversion: true) ?? Data() //"vScQ9Aec2Z8RKNvfZhpg375RWVIN1QMf8x_q9houJnc".getBytes();
        options.userEntity?.name = responseFromAPI?.user?.name ?? "" //"admin";
        options.userEntity?.displayName = responseFromAPI?.user?.displayName ?? "" //"admin";
        options.clientDataHash = generateClientDataHash(
            challenge: responseFromAPI?.challenge,
            type: "webauthn.create",
            origin: origin
        ) ?? Data()
        options.requireResidentKey = false
        options.requireUserPresence = true
        options.requireUserVerification = false
        options.excludeCredentialDescriptorList = [PublicKeyCredentialDescriptor()]
        var credTypesAndPubKeyAlgs: [String: Int] = ["public-key": 7]
        options.credTypesAndPubKeyAlgs = credTypesAndPubKeyAlgs
        return options
    }
    
    func register(responseFromAPI: AttestationOptionResponse?, origin: String?, credentialSource: PublicKeyCredentialSource?) -> AttestationResultRequest? {
        let options = generateAuthenticatorMakeCredentialOptions(responseFromAPI: responseFromAPI, origin: origin)
        
        let attestationObject = makeInternalCredential(options: options, credentialSource: credentialSource)
        
        let clientDataJSON = generateClientDataJSON(challenge: responseFromAPI?.challenge, type: "webauthn.create", origin: origin) ?? ""
        let attestationObjectBytes = attestationObject.asCBOR
        let response = Response(attestationObject: attestationObjectBytes.base64EncodedString(), clientDataJSON: clientDataJSON)
        let attestationResultRequest = AttestationResultRequest(type: "public-key", response: response)
        
        return attestationResultRequest
    }
    
    // MARK: - Private
    
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
