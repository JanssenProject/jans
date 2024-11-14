//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import SwiftCBOR

protocol AttestationObject {
    associatedtype AttStmt // attestation statement

    var fmt: String { get }
    var attStmt: AttStmt? { get }
    var authData: Data { get } // Its name must be authData not authenticatorData

    static func decode(_ data: Data) -> Result<Self, WebAuthnError>

    func toCBOR() -> Result<Data, WebAuthnError>
}

struct NoneAttestationObject: AttestationObject, Codable {
    typealias AttStmt = NoneStmt

    var fmt: String = "none"
    var attStmt: AttStmt?
    let authData: Data

    func toCBOR() -> Result<Data, WebAuthnError> {
        let attObj: [String: Any] = [
            "fmt": self.fmt,
            "attStmt": [:], // when the format is "none", `attStmt` should be empty.
            "authData": self.authData
        ]
        do {
            let cbor = try CBOR.encodeMap(attObj)
            return .success(Data(cbor))
        } catch {
            return .failure(.encodingError(error))
        }
    }

    static func decode(_ data: Data) -> Result<Self, WebAuthnError> {
        do {
            let decoded = try CodableCBORDecoder().decode(Self.self, from: data)
            return .success(decoded)
        } catch {
            return .failure(.encodingError(error))
        }
    }
}
