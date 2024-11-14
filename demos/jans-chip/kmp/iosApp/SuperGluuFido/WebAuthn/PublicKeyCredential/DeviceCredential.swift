//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

/// It manages public key credentials using the device credential authenticator.
///
/// It enables secure user authentication by utilizing biometry or a passcode.
/// If biometry is available, the system uses that first. If not, the system
/// prompts the user for the device passcode or user's password.
public class DeviceCredential: PublicKeyCredential {
    public var aaguid = AuthenticatorType.deviceCredential.aaguid
    public var credSrcStorage: CredentialSourceStorage
    public var keyStorage = KeyStorage(.deviceCredential)
    public var localAuthn: LocalAuthenticationProtocol

    public init(_ db: CredentialSourceStorage, _ localAuthnUIString: String? = nil) {
        self.credSrcStorage = db
        self.localAuthn = LocalAuthentication(.deviceOwnerAuthentication)
        if let localAuthnUIString = localAuthnUIString {
            self.localAuthn.localizedReason = localAuthnUIString
        }
    }
}
