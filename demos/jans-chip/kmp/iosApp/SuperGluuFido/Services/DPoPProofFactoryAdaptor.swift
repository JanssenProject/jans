//
//  DPoPProofFactoryAdaptor.swift
//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 15.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import shared

class DPoPProofFactoryAdaptor: DPoPProofFactoryProvider {
    func getChecksum() -> String {
        ""
    }

    func getIssuerFromClaimsSet() -> String {
        ""
    }

    func getJWKS() -> String? {
        ""
    }

    func getPackageName() -> String {
        ""
    }

    func issueDPoPJWTToken(httpMethod: String, requestUrl: String) -> String? {
        ""
    }

    func issueJWTToken(claims: KotlinMutableDictionary<AnyObject, AnyObject>) -> String? {
        ""
    }
}
