//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct AttestedCredentialData: Codable {
    let aaguid: UUID
    let credentialId: Data
    let publicKey: Data // CBOR-encoded public key

    func toData() -> Data {
        let aaguidData = self.aaguid.toData()
        // According to the spec(https://www.w3.org/TR/webauthn-2/#sctn-attested-credential-data),
        // type of credential identifier length is 16-bit unsigned big-endian integer.
        let credentialIdLengthData = UInt16(credentialId.count).toDataBigEndian()
        return aaguidData + credentialIdLengthData + self.credentialId + publicKey
    }
}
