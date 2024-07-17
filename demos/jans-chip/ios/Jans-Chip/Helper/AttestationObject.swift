//
//  AttestationObject.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 16.07.2024.
//

import Foundation

final class AttestationObject {
    
    let authData: Data?
    
    init(authData: Data?) {
        self.authData = authData
    }
    
    var asCBOR: Data {
        let cBOR = CBOR(authData: authData, fmt: "none", attStmt: "attStmt")
        let jsonEncoder = JSONEncoder()
        if let jsonData = try? jsonEncoder.encode(cBOR) {
            return jsonData
        }
        
        return Data()
    }
}
