//
//  AuthenticationAdaptor.swift
//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 15.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import shared

class AuthenticationAdaptor: AuthenticationProvider {

    func authenticate(assertionOptionResponse: AssertionOptionResponse, origin: String?) async throws -> AssertionResultRequest? {
        nil
    }

    func getAllCredentials() -> [KtPublicKeyCredentialSource]? {
        nil
    }

    func isCredentialsPresent(username: String) -> Bool {
        false
    }

    func register(responseFromAPI: AttestationOptionResponse?, origin: String?) async throws -> AttestationResultRequest? {
        nil
    }
}
