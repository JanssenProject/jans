//
//  AuthAdaptor.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 16.07.2024.
//

import Foundation

final class AuthAdaptor {
    
    private let authenticator = Authenticator()
    
    func getAllCredentials() -> [PublicKeyCredentialSource]? {
        let credentialSafe = CredentialSafe()
        return credentialSafe.allCredentialSource()
    }
    
    func isCredentialsPresent(username: String) -> Bool {
        let allCredentials = getAllCredentials()
        return allCredentials != nil && allCredentials?.filter({ $0.userDisplayName == username }).isEmpty == false
    }
    
    func selectPublicKeyCredentialSource(credentialSelector: CredentialSelector,
                                         assertionOptionResponse: AssertionOptionResponse?,
                                         origin: String?) -> PublicKeyCredentialSource? {
        let options: AuthenticatorGetAssertionOptions = authenticator.generateAuthenticatorGetAssertionOptions(assertionOptionResponse: assertionOptionResponse, origin: origin)
        return authenticator.selectPublicKeyCredentialSource(credentialSelector: credentialSelector, options: options)
    }
    
    func generateSignature(credentialSource: PublicKeyCredentialSource?) {
        if let credentialSource = credentialSource {
            authenticator.generateSignature(credentialSource: credentialSource)
        }
    }
}
