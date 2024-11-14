//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

/// It manages public key credentials using the biometric authenticator.
///
/// It facilitates secure user authentication by leveraging biometric features
/// such as Touch ID or Face ID on supported devices.
public class Biometric: PublicKeyCredential {
    public var aaguid = AuthenticatorType.biometric.aaguid
    public var credSrcStorage: CredentialSourceStorage
    public var keyStorage = KeyStorage(.biometric)
    public var localAuthn: LocalAuthenticationProtocol
    
    public init(_ db: CredentialSourceStorage, _ localAuthnUIString: String? = nil) {
        self.credSrcStorage = db
        self.localAuthn = LocalAuthentication(.deviceOwnerAuthenticationWithBiometrics)
        if let localAuthnUIString = localAuthnUIString {
            self.localAuthn.localizedReason = localAuthnUIString
        }
    }
}
