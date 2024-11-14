//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

extension String {
    func base64UrlToData() -> Data? {
        var base64 = self.replacingOccurrences(of: "-", with: "+")
                         .replacingOccurrences(of: "_", with: "/")
        if base64.count % 4 != 0 {
            base64.append(String(repeating: "=", count: 4 - base64.count % 4))
        }
        return Data(base64Encoded: base64, options: .ignoreUnknownCharacters)
    }

    func toData() -> Data {
        return Data(self.utf8)
    }

    func toSHA256() -> Data {
        return Data(self.utf8).toSHA256()
    }
}
