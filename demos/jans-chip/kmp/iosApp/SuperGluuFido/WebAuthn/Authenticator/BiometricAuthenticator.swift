//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

final class BiometricAuthenticator: Authenticator {
    let type = AuthenticatorType.biometric
    var credSrcStorage: CredentialSourceStorage
    var keyStorage: KeyStorage
    var localAuthn: LocalAuthenticationProtocol

    init(_ db: CredentialSourceStorage, _ ks: KeyStorage, _ la: LocalAuthenticationProtocol) {
        self.credSrcStorage = db
        self.keyStorage = ks
        self.localAuthn = la
    }
}
