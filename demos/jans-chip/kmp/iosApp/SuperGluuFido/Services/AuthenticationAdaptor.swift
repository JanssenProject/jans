//
//  AuthenticationAdaptor.swift
//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 15.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import shared
import LocalAuthentication

class AuthenticationAdaptor: AuthenticationProvider {
    
    let db = WebAuthnCredentialSourceStorage()!
    var credential: PublicKeyCredential {
        switch LAContext().biometricType {
        case .faceID, .touchID, .opticID:
            return Biometric(db)
        default:
            return DeviceCredential(db)
        }
    }
    
    func authenticate(assertionOptionResponse: AssertionOptionResponse, origin: String?) async throws -> AssertionResultRequest? {
        do {
            let registerResult = try await credential.get(assertionOptionResponse: assertionOptionResponse, origin: origin ?? "").get()
            let assertionResultRequest = AssertionResultRequest()
            assertionResultRequest.id = registerResult.id.toBase64Url()
            assertionResultRequest.rawId = registerResult.id.toBase64Url()
            assertionResultRequest.type = "public-key"
            let response = Response()
            response.authenticatorData = registerResult.authenticatorData.toBase64Url()
            response.clientDataJSON = registerResult.clientDataJson.toBase64Url()
            response.signature = registerResult.signature.toBase64Url()
            assertionResultRequest.response = response
            return assertionResultRequest
        } catch {
            print("authentication result: \(error)")
            return nil
        }
    }

    func getAllCredentials() -> [KtPublicKeyCredentialSource]? {
        do {
            let allCredentialSources = try db.loadAll().get()
            return allCredentialSources?.compactMap { credentialSource in
                KtPublicKeyCredentialSource(
                    roomUid: 1,
                    id: KotlinByteArray.from(data: credentialSource.id.toData()),
                    keyPairAlias: credentialSource.aaguid,
                    rpId: credentialSource.rpId,
                    userHandle: KotlinByteArray.from(data: credentialSource.type.toData()),
                    userDisplayName: credentialSource.userDisplayName,
                    otherUI: credentialSource.userName,
                    keyUseCounter: 1)
            }
        } catch {
            return nil
        }
    }
    
    func deleteAllKeys() {
        do {
            try db.deleteAll()
        } catch(let error) {
            print("\(error.localizedDescription)")
        }
    }

    func isCredentialsPresent(username: String) -> Bool {
        do {
            let allCredentialSources = try db.loadAll().get()
            return allCredentialSources?.filter({ $0.userName == username }).isEmpty == false
        } catch {
            return false
        }
    }

    func register(responseFromAPI: AttestationOptionResponse?, origin: String?) async throws -> AttestationResultRequest? {
        guard let responseFromAPI else { return nil }
        guard let origin else { return nil }
        
        do {
            let registerResult = try await credential.create(responseFromAPI: responseFromAPI, origin: origin).get()
            let attestationResponse = AttestationResponse(
                attestationObject: registerResult.attestation.toBase64Url(),
                clientDataJSON: registerResult.clientDataJson.toBase64Url())
            let result = AttestationResultRequest(
                id: registerResult.id.toBase64Url(),
                type: "public-key",
                response: attestationResponse)
            return result
        } catch {
            print("register result: \(error)")
            return nil
        }
    }
}
